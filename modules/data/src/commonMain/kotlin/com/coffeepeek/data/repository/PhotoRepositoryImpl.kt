package com.coffeepeek.data.repository

import com.coffeepeek.api.model.request.PhotoRequestDto
import com.coffeepeek.api.model.request.UploadedPhotoReq
import com.coffeepeek.api.model.response.GenerateUploadUrlDto
import com.coffeepeek.api.service.PhotoApiService
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UploadedPhotoMeta
import com.coffeepeek.domain.repository.PhotoRepository

class PhotoRepositoryImpl(
    private val photoApiService: PhotoApiService,
) : PhotoRepository {

    override suspend fun uploadAvatar(photo: PendingPhotoUpload): Result<UploadedPhotoMeta> =
        uploadSinglePhoto(
            requestUploadUrl = { request ->
                photoApiService.requestAvatarUploadUrl(request).getOrThrow()
            },
            photo = photo,
        )

    override suspend fun uploadShopPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> =
        uploadPhotos(photos) { photoApiService.requestShopPhotoUploadUrls(it).getOrThrow() }

    override suspend fun uploadReviewPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> =
        uploadPhotos(photos, tagging = "is_permanent=False") {
            photoApiService.requestReviewPhotoUploadUrls(it).getOrThrow()
        }

    override suspend fun uploadMenuPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> =
        uploadPhotos(photos) { photoApiService.requestMenuPhotoUploadUrls(it).getOrThrow() }

    override suspend fun uploadCheckInPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> =
        uploadPhotos(photos) { photoApiService.requestCheckInPhotoUploadUrls(it).getOrThrow() }

    override suspend fun uploadRoasterPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>> =
        uploadPhotos(photos) { photoApiService.requestRoasterPhotoUploadUrls(it).getOrThrow() }

    private suspend fun uploadPhotos(
        photos: List<PendingPhotoUpload>,
        tagging: String? = null,
        requestUrls: suspend (List<PhotoRequestDto>) -> List<GenerateUploadUrlDto>,
    ): Result<List<UploadedPhotoMeta>> = runCatching {
        if (photos.isEmpty()) return@runCatching emptyList()

        val requests = photos.map { photo ->
            PhotoRequestDto(
                sizeBytes = photo.bytes.size,
                fileName = photo.fileName,
                contentType = photo.contentType,
            )
        }

        val uploadUrls = requestUrls(requests)
        if (uploadUrls.size != photos.size) {
            error("Несовпадение числа URL для загрузки фото")
        }

        uploadUrls.zip(photos).forEach { (urlDto, photo) ->
            photoApiService.uploadToPresignedUrl(
                uploadUrl = urlDto.uploadUrl,
                bytes = photo.bytes,
                contentType = photo.contentType,
                tagging = tagging,
            ).getOrThrow()
        }

        uploadUrls.zip(photos).map { (urlDto, photo) ->
            UploadedPhotoMeta(
                fileName = photo.fileName,
                contentType = photo.contentType,
                storageKey = urlDto.storageKey,
                size = photo.bytes.size.toLong(),
            )
        }
    }

    private suspend fun uploadSinglePhoto(
        requestUploadUrl: suspend (PhotoRequestDto) -> com.coffeepeek.api.model.response.GenerateUploadUrlDto,
        photo: PendingPhotoUpload,
    ): Result<UploadedPhotoMeta> = runCatching {
        val request = PhotoRequestDto(
            sizeBytes = photo.bytes.size,
            fileName = photo.fileName,
            contentType = photo.contentType,
        )
        val urlDto = requestUploadUrl(request)
        photoApiService.uploadToPresignedUrl(
            uploadUrl = urlDto.uploadUrl,
            bytes = photo.bytes,
            contentType = photo.contentType,
        ).getOrThrow()
        UploadedPhotoMeta(
            fileName = photo.fileName,
            contentType = photo.contentType,
            storageKey = urlDto.storageKey,
            size = photo.bytes.size.toLong(),
        )
    }
}

internal fun List<UploadedPhotoMeta>.toUploadedPhotoReqs(): List<UploadedPhotoReq> = map {
    UploadedPhotoReq(
        fileName = it.fileName,
        contentType = it.contentType,
        storageKey = it.storageKey,
        size = it.size,
    )
}

internal fun List<PendingPhotoUpload>.toUploadedPhotoReqsFromPending(
    uploaded: List<UploadedPhotoMeta>,
): List<UploadedPhotoReq> = uploaded.map {
    UploadedPhotoReq(
        fileName = it.fileName,
        contentType = it.contentType,
        storageKey = it.storageKey,
        size = it.size,
    )
}
