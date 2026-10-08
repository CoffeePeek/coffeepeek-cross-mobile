package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.PhotoRequestDto
import com.coffeepeek.api.model.response.GenerateUploadUrlDto
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.UploadUrlValidator
import com.coffeepeek.api.utils.postResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class PhotoApiService(
    private val client: HttpClient,
    private val uploadClient: HttpClient,
) {

    suspend fun requestAvatarUploadUrl(
        request: PhotoRequestDto,
    ): Result<GenerateUploadUrlDto> = runCatching {
        val response = client.postResult("/api/Photos/avatar") {
            setJsonBody(request)
        }.getOrThrow()
        if (!response.status.isSuccess()) {
            throw ApiException("Не удалось получить URL для аватара (${response.status.value})")
        }
        val apiResponse = response.body<ApiResponse<GenerateUploadUrlDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data.also { UploadUrlValidator.requirePublicUploadUrl(it.uploadUrl) }
    }

    suspend fun requestShopPhotoUploadUrls(
        requests: List<PhotoRequestDto>,
    ): Result<List<GenerateUploadUrlDto>> = requestPhotoUploadUrls("/api/Photos/shop", requests)

    suspend fun requestReviewPhotoUploadUrls(
        requests: List<PhotoRequestDto>,
    ): Result<List<GenerateUploadUrlDto>> = requestPhotoUploadUrls("/api/Photos/review", requests)

    suspend fun requestCheckInPhotoUploadUrls(
        requests: List<PhotoRequestDto>,
    ): Result<List<GenerateUploadUrlDto>> = requestPhotoUploadUrls("/api/Photos/check-in", requests)

    suspend fun requestMenuPhotoUploadUrls(
        requests: List<PhotoRequestDto>,
    ): Result<List<GenerateUploadUrlDto>> = requestPhotoUploadUrls("/api/Photos/menu", requests)

    suspend fun requestRoasterPhotoUploadUrls(
        requests: List<PhotoRequestDto>,
    ): Result<List<GenerateUploadUrlDto>> = requestPhotoUploadUrls("/api/Photos/roaster", requests)

    private suspend fun requestPhotoUploadUrls(
        path: String,
        requests: List<PhotoRequestDto>,
    ): Result<List<GenerateUploadUrlDto>> = runCatching {
        val response = client.postResult(path) {
            setJsonBody(requests)
        }.getOrThrow()
        if (!response.status.isSuccess()) {
            throw ApiException("Не удалось загрузить фото (${response.status.value})")
        }
        val apiResponse = response.body<ApiResponse<List<GenerateUploadUrlDto>>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data.onEach { UploadUrlValidator.requirePublicUploadUrl(it.uploadUrl) }
    }

    suspend fun uploadToPresignedUrl(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String,
        // Signed into /review presigned URLs; must be sent verbatim or MinIO returns 403.
        tagging: String? = null,
    ): Result<Unit> = runCatching {
        UploadUrlValidator.requirePublicUploadUrl(uploadUrl)
        val response = uploadClient.put(uploadUrl) {
            headers {
                remove(HttpHeaders.Accept)
                remove(HttpHeaders.AcceptCharset)
                append(HttpHeaders.ContentType, contentType)
                tagging?.let { append("x-amz-tagging", it) }
            }
            setBody(bytes)
        }
        if (!response.status.isSuccess()) {
            val details = runCatching { response.bodyAsText().take(200) }.getOrDefault("")
            throw ApiException(
                if (details.isNotBlank()) {
                    "Ошибка загрузки фото (${response.status.value}): $details"
                } else {
                    "Ошибка загрузки фото (${response.status.value})"
                },
            )
        }
    }
}
