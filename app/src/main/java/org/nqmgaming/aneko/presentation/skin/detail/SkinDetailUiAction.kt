package org.nqmgaming.aneko.presentation.skin.detail

import android.net.Uri
import org.nqmgaming.aneko.data.skin.SkinConfig

sealed class SkinDetailUiAction {
    data class UpdateSkinPath(val skinPath: Uri?) : SkinDetailUiAction()
    data class UpdateSkinConfig(val skinConfig: SkinConfig?) : SkinDetailUiAction()
}