package org.nqmgaming.aneko.presentation.skin.detail

import android.net.Uri
import org.nqmgaming.aneko.data.skin.SkinConfig

data class SkinDetailUiState(
    val skinPath: Uri = Uri.EMPTY,
    val skinConfig: SkinConfig? = null,
)