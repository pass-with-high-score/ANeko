package org.nqmgaming.aneko.presentation

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper.SHORTCUT_ID_TOGGLE

@AndroidEntryPoint
class ShortcutHandlerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val viewModel: AnekoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        // Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(overlayIntent)
            finish()
            return
        }

        // Check notification permission (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Handle the shortcut action
        handleShortcutIntent(intent)
        finish()
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val currentlyEnabled = prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)

        when (action) {
            ShortcutManagerHelper.ACTION_TOGGLE_FROM_SHORTCUT -> {
                prefs.edit {
                    putBoolean(AnimationService.PREF_KEY_ENABLE, !currentlyEnabled)
                    if (!currentlyEnabled) {
                        putBoolean(AnimationService.PREF_KEY_VISIBLE, true)
                    }
                }
                if (!currentlyEnabled) {
                    viewModel.enableAnimation()
                } else {
                    viewModel.disableAnimation()
                }
                startService(
                    Intent(this, AnimationService::class.java)
                        .setAction(if (!currentlyEnabled) AnimationService.ACTION_START else AnimationService.ACTION_STOP)
                )
                ShortcutManagerHelper.reportShortcutUsed(this, SHORTCUT_ID_TOGGLE)
            }

            ShortcutManagerHelper.ACTION_SKIN_FROM_SHORTCUT -> {
                val skinPackage = intent.getStringExtra(ShortcutManagerHelper.EXTRA_SKIN_PACKAGE)
                if (skinPackage != null) {
                    // Fire-and-forget DB toggle (UI will catch up via Flow)
                    viewModel.onToggleSkin(skinPackage, this)

                    // Synchronously compute new skin slots from current prefs
                    val nekoCount = prefs.getString(AnimationService.PREF_KEY_NEKO_COUNT, "1")
                        ?.toFloatOrNull()?.toInt()?.coerceIn(1, 6) ?: 1

                    // If is full 6/6, show toast and do nothing (can't add more)
                    if (nekoCount == 6 && !prefs.getString(
                            AnimationService.PREF_KEY_SKIN_COMPONENT,
                            ""
                        ).isNullOrBlank() && !prefs.getString("motion.skin.5", null).isNullOrBlank()
                    ) {
                        // Show toast (need to run on UI thread since we're in a service context)
                        runOnUiThread {
                            Toast.makeText(this, R.string.max_neko_reached, Toast.LENGTH_SHORT)
                                .show()
                        }
                        return
                    }

                    // Collect currently active skins from slot prefs
                    val currentSlots = (0 until nekoCount).mapNotNull { i ->
                        prefs.getString("motion.skin.$i", null)
                    }.toMutableList()


                    // Also check the global skin pref as fallback
                    val globalSkin = prefs.getString(AnimationService.PREF_KEY_SKIN_COMPONENT, "")
                    if (currentSlots.isEmpty() && !globalSkin.isNullOrBlank()) {
                        currentSlots.add(globalSkin)
                    }

                    val isSkinActive = currentSlots.contains(skinPackage)

                    if (isSkinActive) {
                        // Remove this skin from slots
                        currentSlots.remove(skinPackage)
                    } else {
                        // Add this skin to slots (max 6)
                        if (currentSlots.size < 6) {
                            currentSlots.add(skinPackage)
                        }
                    }

                    // Synchronously update ALL prefs before starting service
                    prefs.edit {
                        putString(
                            AnimationService.PREF_KEY_NEKO_COUNT,
                            currentSlots.size.coerceAtLeast(1).toString()
                        )
                        if (currentSlots.isNotEmpty()) {
                            putString(
                                AnimationService.PREF_KEY_SKIN_COMPONENT,
                                currentSlots.first()
                            )
                            currentSlots.forEachIndexed { index, pkg ->
                                putString("motion.skin.$index", pkg)
                            }
                        }
                        // Clean up unused slots
                        for (i in currentSlots.size until 6) {
                            remove("motion.skin.$i")
                        }
                    }

                    if (currentSlots.isEmpty()) {
                        // No skins left → stop
                        viewModel.disableAnimation()
                        prefs.edit {
                            putBoolean(AnimationService.PREF_KEY_ENABLE, false)
                            putBoolean(AnimationService.PREF_KEY_VISIBLE, false)
                        }
                        startService(
                            Intent(this, AnimationService::class.java)
                                .setAction(AnimationService.ACTION_STOP)
                        )
                    } else {
                        // Has skins → ensure service is running
                        viewModel.enableAnimation()
                        prefs.edit {
                            putBoolean(AnimationService.PREF_KEY_ENABLE, true)
                            putBoolean(AnimationService.PREF_KEY_VISIBLE, true)
                        }
                        startService(
                            Intent(this, AnimationService::class.java)
                                .setAction(AnimationService.ACTION_START)
                        )
                    }

                    val shortcutId = "skin_${skinPackage.replace("/", "_")}"
                    ShortcutManagerHelper.reportShortcutUsed(this, shortcutId)
                }
            }
        }
    }
}
