package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.RappelQuotidienApp

/**
 * Récepteur d'alerte pour les minuteurs d'activité en arrière-plan.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val activityName = intent.getStringExtra("activity_name") ?: "Activité"
        val durationMins = intent.getIntExtra("duration_minutes", 0)
        val durationSeconds = intent.getIntExtra("duration_seconds", durationMins * 60)

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

        val durationText = if (durationSeconds >= 60) {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            if (secs > 0) "${mins} min ${secs} s" else "${mins} min"
        } else {
            "${durationSeconds} s"
        }
        val message = "Temps écoulé pour votre session de \"%s\" (%s) 🌟".format(activityName, durationText)

        val prefs = context.getSharedPreferences("rappel_quotidien_prefs", Context.MODE_PRIVATE)
        val soundUriStr = prefs.getString("completion_sound_uri", null)

        val builder = NotificationCompat.Builder(context, RappelQuotidienApp.CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Minuteur Terminé ! ⏱️")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (soundUriStr != null) {
            val customUri = Uri.parse(soundUriStr)
            builder.setSound(customUri)
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
            
            // Re-play explicitly to ensure it plays
            try {
                val ringtone = RingtoneManager.getRingtone(context, customUri)
                ringtone?.play()
            } catch (e: Exception) {
                // fallback
            }
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                ringtone?.play()
            } catch (e: Exception) {
                // fallback
            }
        }

        notificationManager.notify(999, builder.build())

        // Nettoyer l'état dans SharedPreferences
        prefs.edit()
            .remove("timer_target_millis")
            .remove("timer_activity_name")
            .remove("timer_duration_mins")
            .apply()
    }
}
