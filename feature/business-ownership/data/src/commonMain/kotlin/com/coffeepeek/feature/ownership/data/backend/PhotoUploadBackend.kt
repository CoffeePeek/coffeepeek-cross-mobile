package com.coffeepeek.feature.ownership.data.backend

import com.coffeepeek.core.network.requirePublicUploadUrl
import com.coffeepeek.feature.ownership.domain.model.BusinessKind
import com.coffeepeek.feature.ownership.domain.model.BusinessPhoto
import com.coffeepeek.feature.ownership.domain.model.OwnershipFailure
import com.coffeepeek.feature.ownership.domain.model.OwnershipProblem
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

/** Reserves one presigned URL per photo, then sends the bytes there. The upload client carries no account credentials. */
internal class PhotoUploadBackend(private val client: HttpClient, private val uploadClient: HttpClient) {
    suspend fun upload(kind: BusinessKind, photos: List<BusinessPhoto>): List<BusinessPhoto> {
        if (photos.isEmpty()) return emptyList()
        try {
            val bytes = photos.map { checkNotNull(it.bytes) }
            val reply = client.post(if (kind == BusinessKind.CoffeeShop) "/api/Photos/shop" else "/api/Photos/roaster") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(photos.mapIndexed { index, photo -> UploadRequest(bytes[index].size, photo.fileName, photo.contentType) })
            }
            if (!reply.status.isSuccess()) throw OwnershipFailure(OwnershipProblem.Unavailable)
            val targets = reply.body<UploadReply>().takeIf { it.isSuccess }?.data
            if (targets == null || targets.size != photos.size || targets.any { it.storageKey.isBlank() }) {
                throw OwnershipFailure(OwnershipProblem.Unavailable)
            }
            targets.forEach { requirePublicUploadUrl(it.uploadUrl) }
            targets.zip(photos).forEachIndexed { index, (target, photo) ->
                val stored = uploadClient.put(target.uploadUrl) {
                    expectSuccess = false
                    headers {
                        remove(HttpHeaders.Accept)
                        remove(HttpHeaders.AcceptCharset)
                        append(HttpHeaders.ContentType, photo.contentType)
                    }
                    setBody(bytes[index])
                }
                if (!stored.status.isSuccess()) throw OwnershipFailure(OwnershipProblem.Unavailable)
            }
            return targets.zip(photos).mapIndexed { index, (target, photo) ->
                photo.copy(bytes = null, storageKey = target.storageKey, size = bytes[index].size.toLong())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: OwnershipFailure) {
            throw failure
        } catch (_: Exception) {
            throw OwnershipFailure(OwnershipProblem.Unavailable)
        }
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
