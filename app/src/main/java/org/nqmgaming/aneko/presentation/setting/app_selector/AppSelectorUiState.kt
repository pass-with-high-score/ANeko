package org.nqmgaming.aneko.presentation.setting.app_selector

import android.content.pm.ResolveInfo

data class AppSelectorUiState (
    val launchableApps: List<ResolveInfo> = emptyList(),
    val enabledPackageNames: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
)