package com.example.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.net.Uri
import android.media.RingtoneManager
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RappelQuotidienApp
import com.example.data.*
import com.example.receiver.ReminderAlarmReceiver
import com.example.receiver.TimerAlarmReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository.getRepository(application)
    private val prefs = application.getSharedPreferences("rappel_quotidien_prefs", Context.MODE_PRIVATE)

    // --- ÉTAT DU SON DE NOTIFICATION DE COMPLÉTION ---
    val completionSoundUri = MutableStateFlow<String?>(null)
    val completionSoundName = MutableStateFlow("Son par défaut")

    init {
        completionSoundUri.value = prefs.getString("completion_sound_uri", null)
        completionSoundName.value = prefs.getString("completion_sound_name", "Son par défaut") ?: "Son par défaut"
    }

    fun saveCustomSound(uri: String?, name: String) {
        prefs.edit()
            .putString("completion_sound_uri", uri)
            .putString("completion_sound_name", name)
            .apply()
        completionSoundUri.value = uri
        completionSoundName.value = name
    }

    // --- ÉTAT DE LA PLANIFICATION ---
    var selectedPeriodType = MutableStateFlow("DAY") // "DAY", "WEEK", "MONTH"
    var selectedDayKey = MutableStateFlow("")  // Format "yyyy-MM-dd"
    var selectedWeekKey = MutableStateFlow("") // Format "yyyy-'W'ww"
    var selectedMonthKey = MutableStateFlow("") // Format "yyyy-MM"

    // Récupérer la date actuelle par défaut au démarrage
    init {
        val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfWeek = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val now = Date()
        selectedDayKey.value = sdfDay.format(now)
        selectedWeekKey.value = sdfWeek.format(now)
        selectedMonthKey.value = sdfMonth.format(now)
    }

    val allTasks: Flow<List<PlanningTask>> = repository.getAllTasks()

    // Flux de tâches filtré réactivement par période sélectionnée
    val currentTasks: StateFlow<List<PlanningTask>> = combine(
        selectedPeriodType,
        selectedDayKey,
        selectedWeekKey,
        selectedMonthKey,
        allTasks
    ) { pType, day, week, month, tasks ->
        val key = when (pType) {
            "DAY" -> day
            "WEEK" -> week
            "MONTH" -> month
            else -> day
        }
        tasks.filter { it.periodType == pType && it.periodKey == key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTask(title: String) {
        val pType = selectedPeriodType.value
        val key = when (pType) {
            "DAY" -> selectedDayKey.value
            "WEEK" -> selectedWeekKey.value
            "MONTH" -> selectedMonthKey.value
            else -> selectedDayKey.value
        }
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                PlanningTask(
                    title = title,
                    periodType = pType,
                    periodKey = key
                )
            )
            com.example.receiver.SportTaskAppWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun toggleTaskCompletion(task: PlanningTask) {
        viewModelScope.launch {
            val newlyCompleted = !task.isCompleted
            repository.updateTask(task.copy(isCompleted = newlyCompleted))
            if (newlyCompleted) {
                playCompletionSoundAndNotification(task.title, isTask = true)
            }
            com.example.receiver.SportTaskAppWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deleteTask(task: PlanningTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            com.example.receiver.SportTaskAppWidgetProvider.triggerUpdate(getApplication())
        }
    }


    // --- ÉTAT DU SPORT JOURNALIER ---
    // Activité sportive selon le jour de la semaine
    val dailyProposedSportName: String
        get() {
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return when (dayOfWeek) {
                Calendar.MONDAY -> "15 Squats & 10 Pompes"
                Calendar.TUESDAY -> "20 Abdos Crunch & Gainage gain de 45s"
                Calendar.WEDNESDAY -> "5 minutes d'Étirements complets du dos"
                Calendar.THURSDAY -> "10 Pompes & 15 Fentes"
                Calendar.FRIDAY -> "30 Jumping Jacks & 20 Squats"
                Calendar.SATURDAY -> "15 Pompes & Gainage de 60s"
                Calendar.SUNDAY -> "8 minutes de Respiration de bien-être & Stretching"
                else -> "10 Squats & 10 fentes"
            }
        }

    val todayDateKey: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

    val todaySportProgress: StateFlow<SportProgress?> = repository.getSportProgressByDateFlow(todayDateKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Historique complet pour les statistiques et graphiques
    val allSportProgress: StateFlow<List<SportProgress>> = repository.getAllSportProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calcul de la série de jours consécutifs (streak)
    val sportStreak: StateFlow<Int> = allSportProgress.map { list ->
        calculateStreak(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleTodaySport() {
        viewModelScope.launch {
            val current = repository.getSportProgressByDate(todayDateKey)
            if (current != null) {
                val newlyCompleted = !current.isCompleted
                repository.saveSportProgress(
                    current.copy(
                        isCompleted = newlyCompleted,
                        completionTime = if (newlyCompleted) System.currentTimeMillis() else 0L
                    )
                )
                if (newlyCompleted) {
                    playCompletionSoundAndNotification(current.exerciseName, isTask = false)
                }
            } else {
                repository.saveSportProgress(
                    SportProgress(
                        dateKey = todayDateKey,
                        exerciseName = dailyProposedSportName,
                        isCompleted = true,
                        completionTime = System.currentTimeMillis()
                    )
                )
                playCompletionSoundAndNotification(dailyProposedSportName, isTask = false)
            }
            com.example.receiver.SportTaskAppWidgetProvider.triggerUpdate(getApplication())
        }
    }

    private fun calculateStreak(activities: List<SportProgress>): Int {
        val completedDates = activities.filter { it.isCompleted }.map { it.dateKey }.toSet()
        if (completedDates.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var streak = 0

        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        // On commence à checker aujourd'hui ou hier
        var checkDate = if (completedDates.contains(todayStr)) todayStr else yesterdayStr

        try {
            cal.time = sdf.parse(checkDate) ?: Date()
            while (true) {
                val dateStr = sdf.format(cal.time)
                if (completedDates.contains(dateStr)) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Erreur calcul streak", e)
        }
        return streak
    }


    // --- ÉTAT DU SOMMEIL ---
    val sleepSummaries: StateFlow<List<SleepSummary>> = repository.getRecentSleepSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reglage de l'agenda typique de nuit dans SharedPreferences
    val bedtimeHourState = MutableStateFlow(23)
    val bedtimeMinuteState = MutableStateFlow(0)
    val wakeupHourState = MutableStateFlow(7)
    val wakeupMinuteState = MutableStateFlow(0)

    init {
        bedtimeHourState.value = prefs.getInt("bedtime_hour", 23)
        bedtimeMinuteState.value = prefs.getInt("bedtime_minute", 0)
        wakeupHourState.value = prefs.getInt("wakeup_hour", 7)
        wakeupMinuteState.value = prefs.getInt("wakeup_minute", 0)
    }

    fun saveSleepSchedule(bHour: Int, bMin: Int, wHour: Int, wMin: Int) {
        prefs.edit()
            .putInt("bedtime_hour", bHour)
            .putInt("bedtime_minute", bMin)
            .putInt("wakeup_hour", wHour)
            .putInt("wakeup_minute", wMin)
            .apply()
        bedtimeHourState.value = bHour
        bedtimeMinuteState.value = bMin
        wakeupHourState.value = wHour
        wakeupMinuteState.value = wMin
    }

    // Déclencheur manuel rapide pour l'analyse du sommeil (aide l'interaction)
    fun runSleepAnalysisManually() {
        viewModelScope.launch {
            // Se base sur la configuration actuelle
            val calendar = Calendar.getInstance()
            val endCal = calendar.clone() as Calendar
            endCal.set(Calendar.HOUR_OF_DAY, wakeupHourState.value)
            endCal.set(Calendar.MINUTE, wakeupMinuteState.value)
            endCal.set(Calendar.SECOND, 0)
            endCal.set(Calendar.MILLISECOND, 0)

            val startCal = calendar.clone() as Calendar
            startCal.add(Calendar.DAY_OF_YEAR, -1)
            startCal.set(Calendar.HOUR_OF_DAY, bedtimeHourState.value)
            startCal.set(Calendar.MINUTE, bedtimeMinuteState.value)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val result = com.example.worker.SleepAnalysisWorker.runSleepAnalysis(
                repository,
                startCal.timeInMillis,
                endCal.timeInMillis
            )

            if (result != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateKey = sdf.format(endCal.time)
                repository.insertSleepSummary(
                    SleepSummary(
                        dateKey = dateKey,
                        bedtimeMillis = result.bedtime,
                        wakeTimeMillis = result.waketime,
                        durationMinutes = result.durationMinutes,
                        efficiency = result.efficiency
                    )
                )
            }
        }
    }


    // --- ÉTAT DES RAPPELS ---
    val allReminders: StateFlow<List<Reminder>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(hour: Int, minute: Int, message: String) {
        viewModelScope.launch {
            val size = allReminders.value.size
            val reminder = Reminder(
                hour = hour,
                minute = minute,
                message = message,
                isEnabled = true
            )
            repository.insertReminder(reminder)
            // On replanifie tous ou on attend que Room nous donne l'ID auto-généré.
            // Pour être sûr, à la mise à jour de la table, les alarmes seront planifiées via UI ou actions directes.
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            repository.updateReminder(updated)
            if (updated.isEnabled) {
                ReminderAlarmReceiver.schedule(getApplication(), updated)
            } else {
                ReminderAlarmReceiver.cancel(getApplication(), updated.id)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderAlarmReceiver.cancel(getApplication(), reminder.id)
            repository.deleteReminder(reminder)
        }
    }

    fun synchronizeReminders() {
        // Recupère et s'assure que toutes les alarmes actives sont bien enregistrées dans l'AlarmManager de l'OS
        viewModelScope.launch {
            allReminders.value.forEach { rem ->
                if (rem.isEnabled) {
                    ReminderAlarmReceiver.schedule(getApplication(), rem)
                } else {
                    ReminderAlarmReceiver.cancel(getApplication(), rem.id)
                }
            }
        }
    }


    // --- ÉTAT DU MINUTEUR ---
    var timerActivityName = MutableStateFlow("Méditation")
    var timerDurationMinutes = MutableStateFlow(5)
    var timerRemainingSeconds = MutableStateFlow(0)
    var timerIsRunning = MutableStateFlow(false)

    private var timerJob: Job? = null

    init {
        // Restauration d'un minuteur actif après fermeture de l'application
        val targetMillis = prefs.getLong("timer_target_millis", 0L)
        if (targetMillis > System.currentTimeMillis()) {
            val savedActivity = prefs.getString("timer_activity_name", "Méditation") ?: "Méditation"
            val savedDur = prefs.getInt("timer_duration_mins", 5)
            timerActivityName.value = savedActivity
            timerDurationMinutes.value = savedDur
            timerIsRunning.value = true
            
            val remainingSecs = ((targetMillis - System.currentTimeMillis()) / 1000).toInt()
            timerRemainingSeconds.value = remainingSecs
            startLocalTicking()
        }
    }

    fun startTimer(activityName: String, minutes: Int) {
        timerJob?.cancel()
        timerActivityName.value = activityName
        timerDurationMinutes.value = minutes
        
        val durationSeconds = minutes * 60
        timerRemainingSeconds.value = durationSeconds
        timerIsRunning.value = true

        val targetMillis = System.currentTimeMillis() + (durationSeconds * 1000L)
        
        // Sauvegarde de l'état pour restauration
        prefs.edit()
            .putLong("timer_target_millis", targetMillis)
            .putString("timer_activity_name", activityName)
            .putInt("timer_duration_mins", minutes)
            .apply()

        // Enregistrement d'un réveil précis AlarmManager pour l'arrière-plan
        scheduleTimerBackgroundReceiver(activityName, minutes, targetMillis)

        startLocalTicking()
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerIsRunning.value = false
        timerRemainingSeconds.value = 0

        // Suppression de l'arrière-plan
        cancelTimerBackgroundReceiver()

        prefs.edit()
            .remove("timer_target_millis")
            .remove("timer_activity_name")
            .remove("timer_duration_mins")
            .apply()
    }

    private fun startLocalTicking() {
        timerJob = viewModelScope.launch {
            while (timerRemainingSeconds.value > 0) {
                delay(1000)
                timerRemainingSeconds.value = timerRemainingSeconds.value - 1
            }
            timerIsRunning.value = false
        }
    }

    private fun scheduleTimerBackgroundReceiver(activityName: String, durationMins: Int, targetMillis: Long) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            putExtra("activity_name", activityName)
            putExtra("duration_minutes", durationMins)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        }
    }

    private fun cancelTimerBackgroundReceiver() {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun playCompletionSoundAndNotification(name: String, isTask: Boolean) {
        val context = getApplication<Application>()
        playCustomSound(context)
        sendLocalCompletionNotification(context, name, isTask)
    }

    fun playCustomSound(context: Context) {
        try {
            val soundUriStr = prefs.getString("completion_sound_uri", null)
            val uri = if (soundUriStr != null) {
                Uri.parse(soundUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed to play completion sound", e)
        }
    }

    private fun sendLocalCompletionNotification(context: Context, name: String, isTask: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val id = if (isTask) 201 else 202
        
        val title = if (isTask) "Objectif Atteint ! 🏆" else "Séance de Sport Complétée ! ⚡"
        val message = if (isTask) {
            "Félicitations pour avoir accompli : $name !"
        } else {
            "Génial ! Vous avez terminé : $name. Continuez ainsi ! 🔥"
        }

        val openIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUriStr = prefs.getString("completion_sound_uri", null)
        val builder = NotificationCompat.Builder(context, com.example.RappelQuotidienApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (soundUriStr != null) {
            builder.setSound(Uri.parse(soundUriStr))
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        notificationManager.notify(id, builder.build())
    }
}
