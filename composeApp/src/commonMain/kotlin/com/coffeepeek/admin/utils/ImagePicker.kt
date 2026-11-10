package com.coffeepeek.admin.utils

import androidx.compose.runtime.Composable

const val MAX_SHOP_PHOTOS = 10

data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String = "image/jpeg",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedImage) return false
        return fileName == other.fileName &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

class PhotoPickerController(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

@Composable
expect fun rememberPhotoPicker(
    maxSelection: Int,
    isLoading: (Boolean) -> Unit = {},
    onPhotosPicked: (List<PickedImage>) -> Unit,
): PhotoPickerController
