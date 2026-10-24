package com.coffeepeek.feature.homecheckin.impl.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload

/** Composition supplies existing artwork/image decoding; this UI never opens a camera or HTTP client. */
class HomeCheckInEditorMedia(
    val ratingArtwork: @Composable (Modifier) -> Unit,
    val emptyBeanArtwork: @Composable (Modifier) -> Unit,
    val methodArtwork: @Composable (String, Modifier) -> Unit,
    val photo: @Composable (HomePreparationUpload, Modifier, Boolean) -> Unit,
)
