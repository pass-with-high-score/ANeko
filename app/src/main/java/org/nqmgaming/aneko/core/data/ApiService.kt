package org.nqmgaming.aneko.core.data

import kotlinx.coroutines.flow.Flow
import org.nqmgaming.aneko.core.networking.ApiResult
import org.nqmgaming.aneko.data.SkinCollection

interface ApiService {
    suspend fun getSkinCollection(): Flow<ApiResult<List<SkinCollection>>>
}