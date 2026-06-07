package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppRepository
import com.example.data.SleepEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Récepteur dynamique pour enregistrer les événements de verrouillage / déverrouillage de l'écran.
 * Permet d'estimer les heures de veille et de sommeil de l'utilisateur.
 */
class ScreenReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val eventType = when (intent.action) {
            Intent.ACTION_SCREEN_ON -> "SCREEN_ON"
            Intent.ACTION_SCREEN_OFF -> "SCREEN_OFF"
            Intent.ACTION_USER_PRESENT -> "USER_PRESENT" // Déclenche quand l'utilisateur déverrouille l'appareil
            else -> return
        }

        val repository = AppRepository.getRepository(context)
        val timestamp = System.currentTimeMillis()

        receiverScope.launch {
            repository.insertSleepEvent(
                SleepEvent(
                    timestamp = timestamp,
                    eventType = eventType
                )
            )
        }
    }
}
