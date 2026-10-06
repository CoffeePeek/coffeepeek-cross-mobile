package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.UpdateAboutReq
import com.coffeepeek.api.model.request.UpdateAvatarReq
import com.coffeepeek.api.model.request.UpdateUsernameReq
import com.coffeepeek.api.model.request.UploadedPhotoReq
import com.coffeepeek.api.utils.setJsonBody
import com.coffeepeek.api.model.response.AccountDeletionRequestDto
import com.coffeepeek.api.model.response.UserProfileDto
import com.coffeepeek.api.model.response.PublicUserProfileDto
import com.coffeepeek.api.model.response.PublicUserProfileResponseDto
import com.coffeepeek.api.model.response.UsernameUpdateDto
import com.coffeepeek.api.utils.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class UserApiService(private val client: HttpClient) {

    suspend fun getMe(): Result<UserProfileDto> = runCatching {
        val response = client.get("/api/Users/me")
        val apiResponse = response.body<ApiResponse<UserProfileDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getUser(userId: String): Result<PublicUserProfileDto> = runCatching {
        val response = client.get("/api/Users/by-slug/$userId")
        if (!response.status.isSuccess()) throw ApiException("Ошибка загрузки профиля (${response.status.value})")
        response.body<PublicUserProfileResponseDto>().data
    }

    suspend fun requestAccountDeletion(): Result<AccountDeletionRequestDto> = runCatching {
        val response = client.delete("/api/Users/me")
        val apiResponse = response.body<ApiResponse<AccountDeletionRequestDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequestDto?> = runCatching {
        val response = client.get("/api/Users/me/deletion-request")
        if (response.status == HttpStatusCode.NotFound) return@runCatching null
        val apiResponse = response.body<ApiResponse<AccountDeletionRequestDto>>()
        when {
            apiResponse.isSuccess && apiResponse.data != null -> apiResponse.data
            response.status == HttpStatusCode.NotFound -> null
            apiResponse.message.orEmpty().contains("not found", ignoreCase = true) -> null
            else -> throw ApiException(apiResponse.message)
        }
    }

    suspend fun updateUsername(username: String): Result<UsernameUpdateDto> = runCatching {
        val response = client.patch("/api/Users/me/username") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUsernameReq(username))
        }
        if (!response.status.isSuccess()) {
            val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
            throw ApiException(err?.message ?: "Ошибка обновления имени (${response.status.value})")
        }
        val result = response.body<ApiResponse<UsernameUpdateDto>>()
        if (!result.isSuccess || result.data == null) throw ApiException(result.message)
        result.data
    }

    suspend fun updateAbout(about: String): Result<Unit> = runCatching {
        val response = client.patch("/api/Users/me/about") {
            contentType(ContentType.Application.Json)
            setBody(UpdateAboutReq(about))
        }
        if (!response.status.isSuccess()) {
            val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
            throw ApiException(err?.message ?: "Ошибка обновления описания (${response.status.value})")
        }
    }

    suspend fun updateAvatar(uploadedPhoto: UploadedPhotoReq): Result<Unit> = runCatching {
        val response = client.patch("/api/Users/me/avatar") {
            setJsonBody(UpdateAvatarReq(uploadedPhoto))
        }
        if (!response.status.isSuccess()) {
            val err = runCatching { response.body<ApiResponse<Unit>>() }.getOrNull()
            throw ApiException(err?.message ?: "Ошибка обновления аватара (${response.status.value})")
        }
    }
}
