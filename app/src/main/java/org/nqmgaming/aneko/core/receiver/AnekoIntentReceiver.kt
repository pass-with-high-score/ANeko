package org.nqmgaming.aneko.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri
import org.nqmgaming.aneko.core.service.AnimationService
import timber.log.Timber

/**
 * BroadcastReceiver that handles external intents to start and stop ANeko.
 * This allows automation apps like Tasker to control ANeko.
 *
 * Supported actions:
 * - org.nqmgaming.aneko.intent.action.START - Starts ANeko animation
 * - org.nqmgaming.aneko.intent.action.STOP - Stops ANeko animation
 */
class AnekoIntentReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_ANEKO = "org.nqmgaming.aneko.intent.action.START"
        const val ACTION_STOP_ANEKO = "org.nqmgaming.aneko.intent.action.STOP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(
            "${context.packageName}_preferences",
            Context.MODE_PRIVATE
        )

        when (intent.action) {
            ACTION_START_ANEKO -> {
                Timber.d("Received START intent from external app")
                startAneko(context, prefs)
            }
            ACTION_STOP_ANEKO -> {
                Timber.d("Received STOP intent from external app")
                stopAneko(context, prefs)
            }
        }
    }

    private fun startAneko(context: Context, prefs: SharedPreferences) {
        // Check for overlay permission
        if (!Settings.canDrawOverlays(context)) {
            Timber.w("Cannot start ANeko: Overlay permission not granted")
            // Optionally, we could open the permission settings
            // but from a BroadcastReceiver context, this may not be ideal
            return
        }

        // Enable the animation
        prefs.edit {
            putBoolean(AnimationService.PREF_KEY_ENABLE, true)
            putBoolean(AnimationService.PREF_KEY_VISIBLE, true)
        }

        // Start the service
        val serviceIntent = Intent(context, AnimationService::class.java).apply {
            action = AnimationService.ACTION_START
        }
        context.startService(serviceIntent)
        
        Timber.d("ANeko started via external intent")
    }

    private fun stopAneko(context: Context, prefs: SharedPreferences) {
        // Disable the animation
        prefs.edit {
            putBoolean(AnimationService.PREF_KEY_ENABLE, false)
            putBoolean(AnimationService.PREF_KEY_VISIBLE, false)
        }

        // Stop the service
        val serviceIntent = Intent(context, AnimationService::class.java).apply {
            action = AnimationService.ACTION_STOP
        }
        context.startService(serviceIntent)
        
        Timber.d("ANeko stopped via external intent")
    }
}
