package org.nqmgaming.aneko.presentation

import android.app.Application
import android.content.ComponentName
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import org.nqmgaming.aneko.core.service.AnimationService
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nqmgaming.aneko.data.skin.SkinConfig
import javax.inject.Inject

@HiltViewModel
class AnekoViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    companion object {
        const val PREF_KEY_THEME = "theme"
    }

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                AnimationService.PREF_KEY_ENABLE -> {
                    _isEnabledState.value = sharedPreferences.getBoolean(key, false)
                }
                PREF_KEY_THEME -> {
                    _isDarkTheme.value = sharedPreferences.getString(key, "light") == "dark"
                }
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val _isEnabledState =
        MutableStateFlow(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false))
    val isEnabledState: StateFlow<Boolean> = _isEnabledState.asStateFlow()

    private val _isDarkTheme =
        MutableStateFlow(prefs.getString(PREF_KEY_THEME, "light") == "dark")
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isFabOpen = MutableStateFlow(false)
    val isFabOpen: StateFlow<Boolean> = _isFabOpen.asStateFlow()

    private val _skinConfig = MutableStateFlow<SkinConfig?>(null)
    val skinConfig: StateFlow<SkinConfig?> = _skinConfig.asStateFlow()

    private val extractedDir = MutableStateFlow<String?>(null)
    val skinDir: StateFlow<String?> = extractedDir.asStateFlow()

    fun toggleTheme() {
        val newTheme = if (_isDarkTheme.value) "light" else "dark"
        prefs.edit { putString(PREF_KEY_THEME, newTheme) }
    }

    fun toggleFabState() {
        _isFabOpen.value = !_isFabOpen.value
    }

    fun updateAnimationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_ENABLE, enabled) }
    }

    fun updateSkin(componentName: ComponentName) {
        prefs.edit {
            putString(
                AnimationService.PREF_KEY_SKIN_COMPONENT,
                componentName.flattenToString()
            )
        }
    }

    fun disableAnimation() {
        prefs.edit {
            putBoolean(
                AnimationService.PREF_KEY_VISIBLE,
                false
            )
        }
    }

    fun enableAnimation() {
        prefs.edit {
            putBoolean(
                AnimationService.PREF_KEY_VISIBLE,
                true
            )
        }
    }

    fun updateNotificationPermission(granted: Boolean) {
        prefs.edit {
            putBoolean(
                AnimationService.PREF_KEY_NOTIFICATION_ENABLE,
                granted
            )
        }
    }

    fun updateSkinConfig(config: SkinConfig?) {
        _skinConfig.value = config
    }

    fun updateSkinDir(dir: String?) {
        extractedDir.value = dir
    }
}