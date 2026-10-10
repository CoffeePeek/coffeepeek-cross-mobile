package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.shop.impl.ui.ShopReviewEditViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormEvent

/** Runtime adapter; opening published photos and handling success belong to the caller. */
@Composable
internal fun ShopReviewEditSheet(
    viewModel: ShopReviewEditViewModel,
    onDismiss: () -> Unit,
    onEvent: (ShopReviewFormEvent) -> Unit,
    onOpenExistingPhoto: (List<String>, Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(viewModel) { viewModel.events.collect(currentOnEvent) }
    ShopReviewEditorSheet(
        state = state,
        onAction = viewModel::onAction,
        onDismiss = onDismiss,
        onPickFromGallery = {},
        onTakePhoto = {},
        onOpenExistingPhoto = onOpenExistingPhoto,
    )
}
