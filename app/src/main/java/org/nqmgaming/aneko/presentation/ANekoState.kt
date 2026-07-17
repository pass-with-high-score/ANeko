package org.nqmgaming.aneko.presentation

import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.data.SkinCollection

data class ANekoState (
    val skins: List<SkinEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val skinCollections: List<SkinCollection>? = emptyList(),
    val isPetdexLoading: Boolean = false,
    val isPetdexRefreshing: Boolean = false,
    val petdexCollections: List<SkinCollection>? = null,
    val newAvailableSkins: List<SkinCollection> = emptyList(),
)
