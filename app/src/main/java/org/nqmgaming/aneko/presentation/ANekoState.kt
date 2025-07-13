package org.nqmgaming.aneko.presentation

import org.nqmgaming.aneko.data.SkinInfo

data class ANekoState (
    val skinList: List<SkinInfo> = emptyList(),
    val selectedIndex : Int = 0,
)