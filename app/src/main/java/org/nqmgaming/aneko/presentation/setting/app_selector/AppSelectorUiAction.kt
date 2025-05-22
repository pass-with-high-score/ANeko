package org.nqmgaming.aneko.presentation.setting.app_selector

sealed class AppSelectorUiAction {
    data class OnEnableApp(val packageName: String, val isEnabled: Boolean) : AppSelectorUiAction()
    data object OnReset : AppSelectorUiAction()
}