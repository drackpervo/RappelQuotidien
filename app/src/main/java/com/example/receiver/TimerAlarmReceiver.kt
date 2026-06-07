package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.RappelQuotidienApp

/**
 * Récepteur d'alerte pour les minuteurs d'activité en arrière-plan.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val activityName = intent.getStringExtra("activity_name") ?: "Activité"
        val durationMins = intent.getIntExtra("duration_minutes", 5)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Temps écoulé pour votre session de \"%s\" (%d min) 🌟".format(activityName, durationMins)

        val builder = NotificationCompat.Builder(context, RappelQuotidienApp.CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Minuteur Terminé ! ⏱️")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(999, builder.build())

        // Nettoyer l'état dans SharedPreferences
        val prefs = context.getSharedPreferences("rappel_quotidien_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("timer_target_millis")
            .remove("timer_activity_name")
            .remove("timer_duration_mins")
            .apply()
    }
}
