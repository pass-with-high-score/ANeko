package org.nqmgaming.aneko.presentation.explore

import org.nqmgaming.aneko.data.SkinCollection

data class ExploreState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val skinCollections: List<SkinCollection>? = emptyList(),
)