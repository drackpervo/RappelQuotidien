package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PlanningTask
import com.example.data.SportProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SportTaskAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val TAG = "SportTaskWidget"

        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, SportTaskAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (allWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, SportTaskAppWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed triggering widget update", e)
            }
        }

        private fun updateWidgetContent(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.sport_task_widget)

            // Configurer le clic pour ouvrir l'application
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Charger les données de la base en tâche de fond (Dispatchers.IO)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)

                    // 1. Charger sport streak & sport progress aujourd'hui
                    val allSport = db.sportProgressDao().getAllSportProgress().first()
                    val streak = calculateStreak(allSport)

                    val todaySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val todayStr = todaySdf.format(Calendar.getInstance().time)
                    val todaySport = db.sportProgressDao().getSportProgressByDate(todayStr)

                    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val dailyProposedSportName = when (dayOfWeek) {
                        Calendar.MONDAY -> "15 Squats & 10 Pompes"
                        Calendar.TUESDAY -> "20 Abdos Crunch & Gainage de 45s"
                        Calendar.WEDNESDAY -> "5 min d'Étirements du dos"
                        Calendar.THURSDAY -> "10 Pompes & 15 Fentes"
                        Calendar.FRIDAY -> "30 Jumping Jacks & 20 Squats"
                        Calendar.SATURDAY -> "15 Pompes & Gainage de 60s"
                        Calendar.SUNDAY -> "8 min de Respiration & Stretching"
                        else -> "10 Squats & 10 fentes"
                    }

                    val sportStatusText = if (todaySport != null) {
                        if (todaySport.isCompleted) {
                            "${todaySport.exerciseName} (Complété ✅)"
                        } else {
                            "${todaySport.exerciseName} (En cours ⏳)"
                        }
                    } else {
                        "$dailyProposedSportName (En cours ⏳)"
                    }

                    // 2. Charger les tâches planifiées pour trouver la prochaine
                    val allTasks = db.planningTaskDao().getAllTasks().first()
                    val uncompletedTasks = allTasks.filter { !it.isCompleted }

                    val nextTask = selectNextTask(uncompletedTasks, todayStr)

                    // 3. Mettre à jour les vues
                    val streakText = if (streak > 0) {
                        "Série Sport : $streak jours 🔥"
                    } else {
                        "Série Sport : 0 jour ⚡"
                    }
                    views.setTextViewText(R.id.widget_sport_streak, streakText)
                    views.setTextViewText(R.id.widget_sport_status, sportStatusText)

                    if (nextTask != null) {
                        val periodLabel = when (nextTask.periodType) {
                            "DAY" -> "Objectif du jour 🎯"
                            "WEEK" -> "Objectif de la semaine 📅"
                            "MONTH" -> "Objectif du mois 🗓️"
                            else -> "Tâche planifiée ⏳"
                        }
                        views.setTextViewText(R.id.widget_task_label, periodLabel)
                        views.setTextViewText(R.id.widget_task_desc, nextTask.title)
                    } else {
                        views.setTextViewText(R.id.widget_task_label, "Objectifs accomplis 🏆")
                        views.setTextViewText(R.id.widget_task_desc, "Toutes les tâches sont terminées !")
                    }

                    // Mettre à jour le widget auprès de l'AppWidgetManager
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                } catch (e: Exception) {
                    Log.e(TAG, "Erreur mise à jour widget", e)
                    // Repli informatif
                    views.setTextViewText(R.id.widget_sport_streak, "Série Sport : -- jours")
                    views.setTextViewText(R.id.widget_sport_status, "Erreur de chargement")
                    views.setTextViewText(R.id.widget_task_label, "Suivi de Vie")
                    views.setTextViewText(R.id.widget_task_desc, "Touchez pour synchroniser")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
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

            var checkDate = if (completedDates.contains(todayStr)) todayStr else yesterdayStr

            try {
                cal.time = sdf.parse(checkDate) ?: Calendar.getInstance().time
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
                Log.e(TAG, "Erreur calcul streak", e)
            }
            return streak
        }

        private fun selectNextTask(uncompletedTasks: List<PlanningTask>, todayStr: String): PlanningTask? {
            if (uncompletedTasks.isEmpty()) return null

            // 1. Chercher d'abord une tâche journalière pour aujourd'hui
            val todayTask = uncompletedTasks.find { it.periodType == "DAY" && it.periodKey == todayStr }
            if (todayTask != null) return todayTask

            // 2. Chercher une tâche hebdomadaire pour cette semaine
            val cal = Calendar.getInstance()
            val sdfWeek = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault())
            val currentWeekStr = sdfWeek.format(cal.time)
            val weekTask = uncompletedTasks.find { it.periodType == "WEEK" && it.periodKey == currentWeekStr }
            if (weekTask != null) return weekTask

            // 3. Chercher une tâche mensuelle pour ce mois
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonthStr = sdfMonth.format(cal.time)
            val monthTask = uncompletedTasks.find { it.periodType == "MONTH" && it.periodKey == currentMonthStr }
            if (monthTask != null) return monthTask

            // 4. Prendre la première tâche non complétée par ordre ID
            return uncompletedTasks.sortedBy { it.id }.firstOrNull()
        }
    }
}
