package com.coffeepeek.feature.collections.api

import androidx.compose.ui.graphics.ImageBitmap

/** Application supplies its bounded, orientation-aware platform decoder; export never decodes full originals. */
typealias CollectionPhotoDecoder = suspend (ByteArray) -> ImageBitmap?
