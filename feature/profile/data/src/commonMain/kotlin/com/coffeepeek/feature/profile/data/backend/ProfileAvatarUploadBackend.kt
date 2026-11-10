package com.coffeepeek.feature.profile.data.backend

import com.coffeepeek.core.network.envelopeData
import com.coffeepeek.core.network.requirePublicUploadUrl
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

internal class ProfileAvatarUploadBackend(private val api: HttpClient, private val uploads: HttpClient) {
    suspend fun upload(image: ProfileAvatarImage, ensureOwner: () -> Unit): UploadedAvatar {
        ensureOwner()
        val response = api.post("/api/Photos/avatar") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(AvatarUploadRequest(image.bytes.size, image.fileName, image.contentType))
        }
        check(response.status.isSuccess()) { "Не удалось получить URL для аватара (${response.status.value})" }
        val target = response.envelopeData<AvatarUploadTarget> { IllegalStateException(it) }
        ensureOwner()
        requirePublicUploadUrl(target.uploadUrl)
        store(target, image)
        ensureOwner()
        return UploadedAvatar(image.fileName, image.contentType, target.storageKey, image.bytes.size.toLong())
    }

    private suspend fun store(target: AvatarUploadTarget, image: ProfileAvatarImage) {
        val response = uploads.put(target.uploadUrl) {
            expectSuccess = false
            headers {
                remove(HttpHeaders.Accept)
                remove(HttpHeaders.AcceptCharset)
                append(HttpHeaders.ContentType, image.contentType)
            }
            setBody(image.bytes)
        }
        check(response.status.isSuccess()) { "Ошибка загрузки фото (${response.status.value})" }
    }
}
