package com.coffeepeek.feature.homecheckin.data.backend

import com.coffeepeek.core.network.requirePublicUploadUrl
import com.coffeepeek.feature.homecheckin.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

internal class HomePreparationUploadBackend(
    private val gateway: HttpClient,
    private val uploader: HttpClient,
) {
    suspend fun upload(photos: List<HomePreparationUpload>): List<UploadedPhotoDto> {
        if (photos.isEmpty()) return emptyList()
        return homeRequestResult {
            require(photos.all { it.fileName.isNotBlank() && it.bytes.isNotEmpty() && it.contentType.startsWith("image/") })
            val targets = gateway.post("/api/Photos/check-in") {
                contentType(ContentType.Application.Json)
                setBody(photos.map { PhotoUploadRequestDto(it.bytes.size, it.fileName, it.contentType) })
            }.homeData<List<PhotoUploadUrlDto>>()
            check(targets.size == photos.size && targets.all { it.storageKey.isNotBlank() })
            // Validate the complete reservation before sending any private image bytes.
            targets.forEach { requirePublicUploadUrl(it.uploadUrl) }
            targets.zip(photos).map { (target, photo) ->
                val response = uploader.put(target.uploadUrl) {
                    headers {
                        remove(HttpHeaders.Accept); remove(HttpHeaders.AcceptCharset)
                        append(HttpHeaders.ContentType, photo.contentType)
                        append("x-amz-tagging", "is_permanent=False")
                    }
                    setBody(photo.bytes)
                }
                if (!response.status.isSuccess()) throw HomePreparationFailure(HomePreparationIssue.InvalidPhotos,
                    mapOf(HomePreparationField.Photos to HomePreparationIssue.InvalidPhotos))
                UploadedPhotoDto(photo.fileName, photo.contentType, target.storageKey, photo.bytes.size.toLong())
            }
        }.getOrElse { failure ->
            val safe = failure as HomePreparationFailure
            throw HomePreparationFailure(safe.issue, safe.fieldErrors, HomePreparationOperation.PhotoUpload)
        }
    }
}
