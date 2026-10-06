package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.network.requirePublicUploadUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shared shop-photo upload transport for reviews and check-ins. */
internal class ShopPhotoUploadBackend(
    private val apiClient: HttpClient,
    private val uploadClient: HttpClient,
) {
    suspend fun upload(photos: List<ShopPhotoUpload>): Result<List<UploadedShopPhoto>> = requestResult {
        if (photos.isEmpty()) return@requestResult emptyList()
        val response = apiClient.post("/api/Photos/shop") {
            contentType(ContentType.Application.Json)
            setBody(photos.map { PhotoUploadRequest(it.bytes.size, it.fileName, it.contentType) })
        }.body<PhotoUploadResponse>()
        val urls = response.data
        if (!response.isSuccess || urls == null || urls.size != photos.size) {
            throw ShopPhotoUploadRejected()
        }
        urls.forEach { requirePublicUploadUrl(it.uploadUrl) }
        urls.zip(photos).forEach { (target, photo) ->
            uploadClient.put(target.uploadUrl) {
                headers {
                    remove(HttpHeaders.Accept)
                    remove(HttpHeaders.AcceptCharset)
                    append(HttpHeaders.ContentType, photo.contentType)
                }
                setBody(photo.bytes)
            }
        }
        urls.zip(photos).map { (target, photo) ->
            UploadedShopPhoto(photo.fileName, photo.contentType, target.storageKey,
                photo.bytes.size.toLong())
        }
    }
}

@Serializable
private data class PhotoUploadRequest(
    @SerialName("sizeBytes") val sizeBytes: Int,
    @SerialName("fileName") val fileName: String,
    @SerialName("contentType") val contentType: String,
)

@Serializable
private data class PhotoUploadResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: List<PhotoUploadUrl>? = null,
)

@Serializable
private data class PhotoUploadUrl(
    @SerialName("uploadUrl") val uploadUrl: String,
    @SerialName("storageKey") val storageKey: String,
)

@Serializable
internal data class UploadedShopPhoto(
    @SerialName("fileName") val fileName: String,
    @SerialName("contentType") val contentType: String,
    @SerialName("storageKey") val storageKey: String,
    @SerialName("size") val size: Long,
)

internal data class ShopPhotoUpload(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String,
)

private class ShopPhotoUploadRejected : Exception("Shop photo upload request was rejected")
