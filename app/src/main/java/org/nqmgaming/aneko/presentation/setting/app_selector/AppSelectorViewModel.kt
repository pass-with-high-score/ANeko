package org.nqmgaming.aneko.presentation.setting.app_selector

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.util.extension.getUserLaunchableApps
import javax.inject.Inject

@HiltViewModel
class AppSelectorViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AppSelectorUiState())
    val uiState = _uiState.asStateFlow()

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    init {
        getLaunchableApps()
    }

    fun onEvent(event: AppSelectorUiAction) {
        when (event) {
            is AppSelectorUiAction.OnEnableApp -> {
                val currentSet = _uiState.value.enabledPackageNames.toMutableSet()
                if (event.isEnabled) {
                    currentSet.add(event.packageName)
                } else {
                    currentSet.remove(event.packageName)
                }

                // Save to SharedPreferences
                prefs.edit { putStringSet("enabled_apps", currentSet) }

                // Update state
                _uiState.value = _uiState.value.copy(enabledPackageNames = currentSet)
            }

            AppSelectorUiAction.OnReset -> {
                prefs.edit { remove("enabled_apps") }
                _uiState.value = _uiState.value.copy(enabledPackageNames = emptySet())
            }

            is AppSelectorUiAction.OnSearchQueryChange -> {
                _uiState.value = _uiState.value.copy(searchQuery = event.query)
            }
            is AppSelectorUiAction.OnToggleSearchBar -> {
                _uiState.value = _uiState.value.copy(showSearchBar = event.isEnabled)
            }
        }
    }


    private fun getLaunchableApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val launchableApps = context.getUserLaunchableApps()
            val enabledSet = prefs.getStringSet("enabled_apps", emptySet()) ?: emptySet()

            _uiState.value = AppSelectorUiState(
                launchableApps = launchableApps,
                enabledPackageNames = enabledSet
            )
        }
    }

}