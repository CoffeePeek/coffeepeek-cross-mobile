package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.impl.ui.ShopReviewCreateViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormEvent

/** Runtime adapter; platform gallery/camera ownership stays with application composition. */
@Composable
internal fun ShopReviewCreateSheet(
    viewModel: ShopReviewCreateViewModel,
    onDismiss: () -> Unit,
    onEvent: (ShopReviewFormEvent) -> Unit,
    pickFromGallery: (Int, (List<ShopReviewPhoto>) -> Unit) -> Unit,
    takePhoto: ((List<ShopReviewPhoto>) -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(viewModel) { viewModel.events.collect(currentOnEvent) }
    ShopReviewEditorSheet(
        state = state,
        onAction = viewModel::onAction,
        onDismiss = onDismiss,
        onPickFromGallery = { remaining ->
            pickFromGallery(remaining) { photos ->
                viewModel.onAction(ShopReviewFormAction.PhotosAdded(photos))
            }
        },
        onTakePhoto = {
            takePhoto { photos -> viewModel.onAction(ShopReviewFormAction.PhotosAdded(photos)) }
        },
        onOpenExistingPhoto = { _, _ -> },
    )
}
