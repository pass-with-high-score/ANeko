package org.nqmgaming.aneko.core.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.ShortcutHandlerActivity
import timber.log.Timber
import java.io.File

object ShortcutManagerHelper {

    const val SHORTCUT_ID_TOGGLE = "toggle_aneko"

    const val ACTION_TOGGLE_FROM_SHORTCUT = "org.nqmgaming.aneko.action.TOGGLE_FROM_SHORTCUT"
    const val ACTION_SKIN_FROM_SHORTCUT = "org.nqmgaming.aneko.action.SKIN_FROM_SHORTCUT"
    const val EXTRA_SKIN_PACKAGE = "extra_skin_package"
    fun createPinnedSkinShortcut(
        context: Context,
        skinName: String,
        skinPackage: String,
        previewPath: String? = null
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager?.isRequestPinShortcutSupported != true) {
            Timber.w("Pinned shortcuts not supported on this launcher")
            return
        }

        // Create unique shortcut ID for this skin
        val shortcutId = "skin_${skinPackage.replace("/", "_")}"

        // Try to load skin preview as icon, fallback to default
        val icon = try {
            if (previewPath != null) {
                val root = File(context.filesDir, "skins")
                val previewFile = File(File(root, skinPackage), previewPath)
                if (previewFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(previewFile.absolutePath)
                    Icon.createWithBitmap(bitmap)
                } else {
                    Icon.createWithResource(context, R.drawable.sleep2)
                }
            } else {
                Icon.createWithResource(context, R.drawable.sleep2)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load skin preview for shortcut")
            Icon.createWithResource(context, R.drawable.sleep2)
        }

        val skinShortcut = ShortcutInfo.Builder(context, shortcutId)
            .setShortLabel(skinName)
            .setLongLabel("$skinName - ANeko")
            .setIcon(icon)
            .setIntent(
                Intent(context, ShortcutHandlerActivity::class.java).apply {
                    action = ACTION_SKIN_FROM_SHORTCUT
                    putExtra(EXTRA_SKIN_PACKAGE, skinPackage)
                }
            )
            .build()

        try {
            shortcutManager.requestPinShortcut(skinShortcut, null)
            Timber.d("Requested pin skin shortcut: $skinName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create pinned skin shortcut")
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
