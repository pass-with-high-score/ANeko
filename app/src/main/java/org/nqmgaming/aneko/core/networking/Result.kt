package org.nqmgaming.aneko.core.networking

sealed class ApiResult<T>(val data:T?=null, val error:String?=null){
    class Success<T>(data: T):ApiResult<T>(data = data)
    class Error<T>(error: String):ApiResult<T>(error = error)
    class Loading<T>:ApiResult<T>()
}