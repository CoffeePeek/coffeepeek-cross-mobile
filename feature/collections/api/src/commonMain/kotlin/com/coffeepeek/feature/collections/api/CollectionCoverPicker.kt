package com.coffeepeek.feature.collections.api

import androidx.compose.runtime.Composable

/** Prepared local bytes only. Uploading is owned by collection data and starts on Save. */
class CollectionCoverImage(val bytes: ByteArray, val mimeType: String, val fileName: String)

typealias CollectionCoverPicker = @Composable (
    show: Boolean,
    onDismiss: () -> Unit,
    onLoading: (Boolean) -> Unit,
    onPicked: (CollectionCoverImage) -> Unit,
) -> Unit
