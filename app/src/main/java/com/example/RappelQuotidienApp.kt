package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.receiver.ScreenReceiver

class RappelQuotidienApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Configuration des canaux de notifications
        createNotificationChannels()

        // Enregistrement dynamique du récepteur d'événements de l'écran
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(ScreenReceiver(), filter)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal 1 : Rappels & Sport
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Rappels et Sport",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification pour les rappels quotidiens et les défis sportifs"
            }

            // Canal 2 : Sommeil
            val sleepChannel = NotificationChannel(
                CHANNEL_SLEEP,
                "Suivi de Sommeil",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rapports d'analyse quotidienne du sommeil"
            }

            // Canal 3 : Minuteur d'activités
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Minuteur d'Activités",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de fin de minuteur pour le sport ou la méditation"
                setSound(null, null) // On peut gérer le son personnalisé ou vibreurs
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(sleepChannel)
            manager.createNotificationChannel(timerChannel)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "rappel_quotidien_reminders"
        const val CHANNEL_SLEEP = "rappel_quotidien_sleep"
        const val CHANNEL_TIMER = "rappel_quotidien_timer"
    }
}
