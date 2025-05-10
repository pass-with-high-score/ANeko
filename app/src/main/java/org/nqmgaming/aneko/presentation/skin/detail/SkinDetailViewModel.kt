package org.nqmgaming.aneko.presentation.skin.detail

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SkinDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkinDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val skinPath: String = savedStateHandle["skinPath"] ?: ""
        _uiState.update {
            it.copy(
                skinPath = skinPath.toUri(),
            )
        }
    }

    fun onEvent(event: SkinDetailUiAction) {
        when (event) {
            is SkinDetailUiAction.UpdateSkinConfig -> {
                _uiState.update {
                    it.copy(
                        skinConfig = event.skinConfig,
                    )
                }
            }

            is SkinDetailUiAction.UpdateSkinPath -> {
                _uiState.update {
                    it.copy(
                        skinPath = event.skinPath ?: Uri.EMPTY,
                    )
                }
            }
        }
    }
}