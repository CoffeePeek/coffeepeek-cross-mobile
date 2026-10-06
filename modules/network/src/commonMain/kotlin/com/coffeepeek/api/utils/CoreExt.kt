package com.coffeepeek.api.utils

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.DataResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
class ApiException(message: String?) : Exception() {
    override val message: String = message?.takeIf { it.isNotBlank() } ?: "Сервер вернул некорректный ответ"
}

suspend inline fun <reified T : DataResponse> HttpResponse.getResult(): Result<T> {
    return runCatching {
        val apiResponse = body<ApiResponse<T>>()
        if (apiResponse.isSuccess && apiResponse.data != null) {
            apiResponse.data
        } else {
            throw ApiException(apiResponse.message)
        }
    }
}

suspend inline fun <reified T : DataResponse> Result<HttpResponse>.getResult(): Result<T> {
    return runCatching {
        val response = getOrThrow()
        val apiResponse = response.body<ApiResponse<T>>()
        if (apiResponse.isSuccess && apiResponse.data != null) {
            apiResponse.data
        } else {
            throw ApiException(apiResponse.message)
        }
    }
}

suspend inline fun Result<HttpResponse>.getIsSuccessResult(): Result<Boolean> {
    return runCatching {
        val response = getOrThrow()
        val apiResponse = response.body<ApiResponse<Boolean>>()
        apiResponse.data == true
    }
}

suspend inline fun Result<HttpResponse>.getStringResult(): Result<String> {
    return runCatching {
        val response = getOrThrow()
        val apiResponse = response.body<ApiResponse<String>>()
        apiResponse.data as String
    }
}

suspend inline fun <reified T : DataResponse> HttpResponse.getNullableResult(): Result<T?> {
    return runCatching {
        val apiResponse = body<ApiResponse<T>>()
        if (apiResponse.isSuccess) {
            apiResponse.data
        } else {
            throw ApiException(apiResponse.message)
        }
    }
}

suspend inline fun <reified T : DataResponse> Result<HttpResponse>.getNullableResult(): Result<T?> {
    return runCatching {
        val response = getOrThrow()
        val apiResponse = response.body<ApiResponse<T>>()
        if (apiResponse.isSuccess) {
            apiResponse.data
        } else {
            throw ApiException(apiResponse.message)
        }
    }
}

suspend fun HttpResponse.getByteArrayResult(): Result<ByteArray> {
    return runCatching {
        if (status.isSuccess()) {
            this.body<ByteArray>()
        } else {
            val apiResponse = body<ApiResponse<*>>()
            throw ApiException(apiResponse.message)
        }
    }
}

suspend fun Result<HttpResponse>.getByteArrayResult(): Result<ByteArray> {
    return runCatching {
        val response = getOrThrow()
        if (response.status.isSuccess()) {
            response.body<ByteArray>()
        } else {
            val apiResponse = response.body<ApiResponse<*>>()
            throw ApiException(apiResponse.message)
        }
    }
}

inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

suspend fun HttpClient.postResult(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): Result<HttpResponse> {
    return runCatching { post(urlString, block) }
}

suspend fun HttpClient.getResult(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): Result<HttpResponse> {
    return runCatching { get(urlString, block) }
}

suspend fun HttpClient.putResult(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): Result<HttpResponse> {
    return runCatching { put(urlString, block) }
}

suspend fun HttpClient.deleteResult(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): Result<HttpResponse> {
    return runCatching { delete(urlString, block) }
}
