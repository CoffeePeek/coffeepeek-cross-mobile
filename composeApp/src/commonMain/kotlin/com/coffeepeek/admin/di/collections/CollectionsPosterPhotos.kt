package com.coffeepeek.admin.di.collections

import androidx.compose.ui.graphics.ImageBitmap

internal expect suspend fun decodeCollectionPosterPhoto(bytes: ByteArray): ImageBitmap?
