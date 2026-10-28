package com.coffeepeek.feature.homecheckin.impl.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload

/**
 * Composition supplies artwork/image decoding; this UI never opens a camera or HTTP client.
 * The photo renderer's Boolean selects a full-size preview (fit) instead of a cropped thumbnail. [shelfBagImage] draws a
 * protected shelf-bag card by its URL, whole: it sizes itself to the card within the incoming bounds, so the modifier's
 * clip follows the card.
 */
class HomeCheckInEditorMedia(
    val ratingArtwork: @Composable (Modifier) -> Unit,
    val emptyBeanArtwork: @Composable (Modifier) -> Unit,
    val methodArtwork: @Composable (String, Modifier) -> Unit,
    val photo: @Composable (HomePreparationUpload, Modifier, Boolean) -> Unit,
    val shelfBagImage: @Composable (url: String, Modifier) -> Unit = { _, _ -> },
)
