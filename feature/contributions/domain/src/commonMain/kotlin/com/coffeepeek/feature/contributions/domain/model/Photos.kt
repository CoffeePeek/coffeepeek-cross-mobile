package com.coffeepeek.feature.contributions.domain.model

/** A photo picked by the user that is not on the server yet. */
class PendingPhoto(val fileName: String, val contentType: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is PendingPhoto && fileName == other.fileName && contentType == other.contentType && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * (31 * fileName.hashCode() + contentType.hashCode()) + bytes.contentHashCode()
}

/** A photo already stored; the submission refers to it by its storage key. */
data class UploadedPhoto(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
