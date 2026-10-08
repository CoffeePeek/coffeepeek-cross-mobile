package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UploadedPhotoMeta

interface PhotoRepository {
    suspend fun uploadAvatar(photo: PendingPhotoUpload): Result<UploadedPhotoMeta>
    suspend fun uploadShopPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>>
    suspend fun uploadReviewPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>>
    suspend fun uploadCheckInPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>>
    suspend fun uploadMenuPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>>
    suspend fun uploadRoasterPhotos(photos: List<PendingPhotoUpload>): Result<List<UploadedPhotoMeta>>
}
