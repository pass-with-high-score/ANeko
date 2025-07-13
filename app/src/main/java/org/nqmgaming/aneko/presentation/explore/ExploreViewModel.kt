package org.nqmgaming.aneko.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.core.data.ApiService
import org.nqmgaming.aneko.core.networking.ApiResult
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    val apiService: ApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreState())
    val uiState = _uiState.asStateFlow()

    init {
        getSkinCollection()
    }

    fun getSkinCollection(isRefresh: Boolean = false) {
        viewModelScope.launch {
            apiService.getSkinCollection().collect { result ->
                when (result) {
                    is ApiResult.Error<*> -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                skinCollections = emptyList(),
                                isRefreshing = false
                            )
                        }
                    }

                    is ApiResult.Loading<*> -> {
                        _uiState.update {
                            it.copy(
                                isLoading = !isRefresh,
                                isRefreshing = true,
                            )
                        }
                    }

                    is ApiResult.Success<*> -> {
                        _uiState.update {
                            it.copy(
                                skinCollections = result.data,
                                isLoading = false,
                                isRefreshing = false
                            )
                        }

                    }
                }
            }
        }
    }
}