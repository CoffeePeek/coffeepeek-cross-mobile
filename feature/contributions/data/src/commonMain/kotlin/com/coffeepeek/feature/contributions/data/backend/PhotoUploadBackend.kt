package com.coffeepeek.feature.contributions.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.network.requirePublicUploadUrl
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.model.UploadedPhoto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal enum class UploadPurpose(val endpoint: String) {
    Shop("/api/Photos/shop"),
    Menu("/api/Photos/menu"),
    Roaster("/api/Photos/roaster"),
}

/** Asks for one presigned URL per photo, then sends the bytes there. The upload client carries no account credentials. */
internal class PhotoUploadBackend(private val apiClient: HttpClient, private val uploadClient: HttpClient) {
    suspend fun upload(photos: List<PendingPhoto>, purpose: UploadPurpose): Result<List<UploadedPhoto>> = requestResult {
        if (photos.isEmpty()) return@requestResult emptyList()
        try {
            send(photos, purpose)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: ContributionFailure) {
            throw failure
        } catch (_: Exception) {
            throw ContributionFailure(FailureKind.Upload)
        }
    }

    private suspend fun send(photos: List<PendingPhoto>, purpose: UploadPurpose): List<UploadedPhoto> {
        val reply = apiClient.post(purpose.endpoint) {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(photos.map { UploadRequest(it.bytes.size, it.fileName, it.contentType) })
        }
        if (!reply.status.isSuccess()) throw ContributionFailure(FailureKind.Upload)
        val targets = reply.body<UploadReply>().takeIf { it.isSuccess }?.data
        if (targets == null || targets.size != photos.size || targets.any { it.storageKey.isBlank() }) throw ContributionFailure(FailureKind.Upload)
        targets.forEach { requirePublicUploadUrl(it.uploadUrl) }
        targets.zip(photos).forEach { (target, photo) ->
            val stored = uploadClient.put(target.uploadUrl) {
                expectSuccess = false
                headers {
                    remove(HttpHeaders.Accept)
                    remove(HttpHeaders.AcceptCharset)
                    append(HttpHeaders.ContentType, photo.contentType)
                }
                setBody(photo.bytes)
            }
            if (!stored.status.isSuccess()) throw ContributionFailure(FailureKind.Upload)
        }
        return targets.zip(photos).map { (target, photo) -> UploadedPhoto(photo.fileName, photo.contentType, target.storageKey, photo.bytes.size.toLong()) }
    }
}

@Serializable
private data class UploadRequest(
    @SerialName("sizeBytes") val sizeBytes: Int,
    @SerialName("fileName") val fileName: String,
    @SerialName("contentType") val contentType: String,
)

@Serializable
private data class UploadReply(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: List<UploadTarget>? = null,
)

@Serializable
private data class UploadTarget(
    @SerialName("uploadUrl") val uploadUrl: String,
    @SerialName("storageKey") val storageKey: String,
)
