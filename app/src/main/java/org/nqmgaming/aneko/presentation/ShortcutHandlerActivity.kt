package org.nqmgaming.aneko.presentation

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper.SHORTCUT_ID_TOGGLE

/**
 * Transparent activity that handles app shortcuts without displaying UI.
 * Configured with noHistory, excludeFromRecents to avoid appearing in task manager.
 */
class ShortcutHandlerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        // Handle the shortcut action
        handleShortcutIntent(intent)

        // Finish immediately - this activity should never be visible
        finish()
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            ShortcutManagerHelper.ACTION_TOGGLE_FROM_SHORTCUT -> {
                val currentlyEnabled = prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)
                prefs.edit {
                    putBoolean(AnimationService.PREF_KEY_ENABLE, !currentlyEnabled)
                    if (!currentlyEnabled) {
                        putBoolean(AnimationService.PREF_KEY_VISIBLE, true)
                    }
                }
                startService(
                    Intent(this, AnimationService::class.java)
                        .setAction(if (!currentlyEnabled) AnimationService.ACTION_START else AnimationService.ACTION_STOP)
                )
                ShortcutManagerHelper.reportShortcutUsed(this, SHORTCUT_ID_TOGGLE)
            }
        }
    }
}
