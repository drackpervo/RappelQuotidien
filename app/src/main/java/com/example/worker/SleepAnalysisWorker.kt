package com.example.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.RappelQuotidienApp
import com.example.data.AppRepository
import com.example.data.SleepEvent
import com.example.data.SleepSummary
import java.text.SimpleDateFormat
import java.util.*

class SleepAnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = AppRepository.getRepository(applicationContext)

        // Récupération de la plage de sommeil typique depuis les SharedPreferences
        val prefs = applicationContext.getSharedPreferences("rappel_quotidien_prefs", Context.MODE_PRIVATE)
        val bedtimeHour = prefs.getInt("bedtime_hour", 23)
        val bedtimeMinute = prefs.getInt("bedtime_minute", 0)
        val wakeupHour = prefs.getInt("wakeup_hour", 7)
        val wakeupMinute = prefs.getInt("wakeup_minute", 0)

        // Définition de l'intervalle d'analyse : du soir précédent au matin actuel
        val calendar = Calendar.getInstance()
        
        // Matin de réveil (aujourd'hui)
        val endCal = calendar.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, wakeupHour)
        endCal.set(Calendar.MINUTE, wakeupMinute)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        // Soir de coucher (hier)
        val startCal = calendar.clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, -1)
        startCal.set(Calendar.HOUR_OF_DAY, bedtimeHour)
        startCal.set(Calendar.MINUTE, bedtimeMinute)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val startTime = startCal.timeInMillis
        val endTime = endCal.timeInMillis

        // Exécuter l'analyse de manière suspendable
        val result = runSleepAnalysis(repository, startTime, endTime)
        
        // Enregistrer le résumé calculé
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateKey = sdf.format(endCal.time) // Jour de réveil

        if (result != null) {
            val summary = SleepSummary(
                dateKey = dateKey,
                bedtimeMillis = result.bedtime,
                wakeTimeMillis = result.waketime,
                durationMinutes = result.durationMinutes,
                efficiency = result.efficiency
            )
            
            repository.insertSleepSummary(summary)

            // Envoi de notification locale
            sendSleepNotification(
                applicationContext,
                result.durationMinutes,
                result.efficiency
            )
        }

        return Result.success()
    }

    private fun sendSleepNotification(context: Context, durationMinutes: Int, efficiency: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        val message = "Dormeuse / Dormeur ! Vous avez dormi environ %dh %02dmin avec une efficacité estimée de %d%%.".format(hours, mins, efficiency)

        val builder = NotificationCompat.Builder(context, RappelQuotidienApp.CHANNEL_SLEEP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Analyse de votre nuit 🌙")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)

        manager.notify(777, builder.build())
    }

    companion object {
        data class AnalysisResult(
            val bedtime: Long,
            val waketime: Long,
            val durationMinutes: Int,
            val efficiency: Int
        )

        suspend fun runSleepAnalysis(
            repository: AppRepository,
            startTime: Long,
            endTime: Long
        ): AnalysisResult? {
            // Lecture asynchrone des événements
            val events = repository.getSleepEventsSince(startTime).filter { it.timestamp in startTime..endTime }

            // Calcul intelligent
            var deducedBedtime = startTime
            var deducedWaketime = endTime

            if (events.isEmpty()) {
                // Pas d'utilisation du téléphone détectée, sommeil de plomb !
                val duration = ((endTime - startTime) / 60000).toInt()
                return AnalysisResult(startTime, endTime, duration, 100)
            }

            // Trouver le coucher (dernier SCREEN_OFF avant la plus longue période d'inactivité)
            // Trouver le lever (premier SCREEN_ON après la plus longue période d'inactivité)
            // Algorithme par défaut :
            // Nous parcourons les événements triés par ordre chronologique.
            // La période de sommeil principale est la plus longue période continue sans SCREEN_ON.
            val sortedEvents = events.sortedBy { it.timestamp }
            
            var maxGapStart: Long = startTime
            var maxGapEnd: Long = endTime
            var maxGapDuration: Long = 0L

            var lastPoint = startTime
            for (event in sortedEvents) {
                if (event.eventType == "SCREEN_ON") {
                    val gap = event.timestamp - lastPoint
                    if (gap > maxGapDuration) {
                        maxGapDuration = gap
                        maxGapStart = lastPoint
                        maxGapEnd = event.timestamp
                    }
                }
                if (event.eventType == "SCREEN_OFF" || event.eventType == "USER_PRESENT") {
                    lastPoint = event.timestamp
                }
            }
            
            // Dernier intervalle jusqu'à l'heure de réveil typique
            val finalGap = endTime - lastPoint
            if (finalGap > maxGapDuration) {
                maxGapDuration = finalGap
                maxGapStart = lastPoint
                maxGapEnd = endTime
            }

            // Si la plus grande période de sommeil est d'au moins 2 heures
            if (maxGapDuration >= 120 * 60 * 1000) {
                deducedBedtime = maxGapStart
                deducedWaketime = maxGapEnd
            } else {
                // Sinon on prend le premier SCREEN_OFF et le dernier SCREEN_ON
                val screenOff = sortedEvents.firstOrNull { it.eventType == "SCREEN_OFF" }?.timestamp
                val screenOn = sortedEvents.lastOrNull { it.eventType == "SCREEN_ON" }?.timestamp
                if (screenOff != null) deducedBedtime = screenOff
                if (screenOn != null) deducedWaketime = screenOn
            }

            // Retrancher les interactions d'écran allumé pendant cette fenêtre
            var activeMinutes = 0
            var activeStart: Long? = null
            
            for (event in sortedEvents) {
                if (event.timestamp in deducedBedtime..deducedWaketime) {
                    if (event.eventType == "SCREEN_ON") {
                        activeStart = event.timestamp
                    } else if ((event.eventType == "SCREEN_OFF" || event.eventType == "USER_PRESENT") && activeStart != null) {
                        val activeDuration = event.timestamp - activeStart
                        activeMinutes += (activeDuration / 60000).toInt()
                        activeStart = null
                    }
                }
            }

            val totalBedTimeMinutes = ((deducedWaketime - deducedBedtime) / 60000).toInt().coerceAtLeast(1)
            val sleepDurationMinutes = (totalBedTimeMinutes - activeMinutes).coerceAtLeast(30)
            val efficiency = ((sleepDurationMinutes.toFloat() / totalBedTimeMinutes.toFloat()) * 100).toInt().coerceIn(0, 100)

            return AnalysisResult(
                bedtime = deducedBedtime,
                waketime = deducedWaketime,
                durationMinutes = sleepDurationMinutes,
                efficiency = efficiency
            )
        }
    }
}
