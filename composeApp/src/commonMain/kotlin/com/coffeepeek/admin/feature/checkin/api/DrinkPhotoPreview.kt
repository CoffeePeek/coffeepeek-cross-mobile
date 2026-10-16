package com.coffeepeek.admin.feature.checkin.api

import androidx.compose.runtime.Composable
import com.coffeepeek.domain.model.PendingPhotoUpload

/** Shared preview entry point; rendering stays inside the check-in feature. */
@Composable
fun DrinkPhotoPreview(
    state: DrinkPhotoState,
    onSelect: (PendingPhotoUpload) -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) = com.coffeepeek.admin.feature.checkin.ui.DrinkPhotoPreviewContent(state, onSelect, onRetry, onSkip)