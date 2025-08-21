package org.nqmgaming.aneko.presentation

import org.nqmgaming.aneko.core.data.entity.SkinEntity

data class ANekoState (
    val skins: List<SkinEntity> = emptyList(),
)