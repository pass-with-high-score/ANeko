package org.nqmgaming.aneko.core.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.nqmgaming.aneko.core.networking.ApiResult
import org.nqmgaming.aneko.core.util.Constants
import org.nqmgaming.aneko.data.SkinCollection
import timber.log.Timber

class ApiServiceImpl(
    private val httpClient: HttpClient
) : ApiService {
    override suspend fun getSkinCollection(): Flow<ApiResult<List<SkinCollection>>> = flow {
        emit(ApiResult.Loading())
        try {
            val response = httpClient.get(Constants.SKIN_COLLECTION_URL)
            Timber.d("Responses: ${response.status}")
            emit(ApiResult.Success(response.body()))
        } catch (e: Exception) {
            Timber.e(e)
            emit(ApiResult.Error(e.message ?: "Unknown error occurred"))
        }
    }
}