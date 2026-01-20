package org.nqmgaming.aneko.core.shortcuts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.ANekoActivity
import timber.log.Timber

object ShortcutManagerHelper {

    const val SHORTCUT_ID_TOGGLE = "toggle_aneko"

    const val ACTION_TOGGLE_FROM_SHORTCUT = "org.nqmgaming.aneko.action.TOGGLE_FROM_SHORTCUT"

    fun createPinnedShortcut(context: Context, activity: Activity) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager?.isRequestPinShortcutSupported != true) {
            Timber.w("Pinned shortcuts not supported on this launcher")
            return
        }

        val toggleShortcut = ShortcutInfo.Builder(context, SHORTCUT_ID_TOGGLE)
            .setShortLabel(context.getString(R.string.shortcut_toggle_short))
            .setLongLabel(context.getString(R.string.shortcut_toggle_long))
            .setIcon(Icon.createWithResource(context, R.drawable.sleep2))
            .setIntent(
                Intent(context, ANekoActivity::class.java).apply {
                    action = ACTION_TOGGLE_FROM_SHORTCUT
                }
            )
            .build()

        // Create the PendingIntent object only if your app needs to be notified
        // that the user allowed the shortcut to be pinned
        shortcutManager.createShortcutResultIntent(toggleShortcut)

        try {
            shortcutManager.requestPinShortcut(toggleShortcut, null)
            Timber.d("Requested pin shortcut")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create pinned shortcut")
        }
    }

    fun reportShortcutUsed(context: Context, shortcutId: String) {
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.reportShortcutUsed(shortcutId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to report shortcut usage")
        }
    }
}
