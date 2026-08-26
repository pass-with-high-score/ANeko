package org.nqmgaming.aneko.presentation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PermissionKey : NavKey

@Serializable
data object OnboardingSkinKey : NavKey

@Serializable
data object HomeKey : NavKey
@Serializable
data object HomeAppKey : NavKey

@Serializable
data object LanguageKey : NavKey

@Serializable
data object ThemeKey : NavKey

@Serializable
data object ExploreKey: NavKey

@Serializable
data class SkinDetailKey(
    val packageName: String,
    val isOnline: Boolean = false,
) : NavKey