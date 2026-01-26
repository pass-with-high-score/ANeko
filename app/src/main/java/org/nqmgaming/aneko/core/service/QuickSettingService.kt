package org.nqmgaming.aneko.core.service

import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit

class QuickSettingService : TileService() {

    private var tile: Tile? = null
    private lateinit var prefs: SharedPreferences

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AnimationService.PREF_KEY_ENABLE) {
                tile?.let {
                    when {
                        !isServiceAvailable() -> it.state = Tile.STATE_UNAVAILABLE
                        prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false) -> {
                            it.state = Tile.STATE_ACTIVE
                            val intent = Intent(this, AnimationService::class.java).apply {
                                action = AnimationService.ACTION_START
                            }
                            startForegroundService(intent)
                        }

                        else -> it.state = Tile.STATE_INACTIVE
                    }
                    it.updateTile()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        tile = qsTile

        tile?.let {
            it.state = when {
                !isServiceAvailable() -> Tile.STATE_UNAVAILABLE
                prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false) -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            it.updateTile()
        }

        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        tile = qsTile
        if (!isServiceAvailable()) {
            tile?.state = Tile.STATE_UNAVAILABLE
        }
    }

    override fun onClick() {
        super.onClick()
        tile?.let {
            if (it.state != Tile.STATE_UNAVAILABLE) {
                val enable = it.state == Tile.STATE_INACTIVE
                prefs.edit { putBoolean(AnimationService.PREF_KEY_ENABLE, enable) }
            }
        }
    }

    private fun isServiceAvailable(): Boolean {
        return Settings.canDrawOverlays(this)
    }
}
