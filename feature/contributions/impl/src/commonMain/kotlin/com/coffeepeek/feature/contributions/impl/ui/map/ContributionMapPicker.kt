package com.coffeepeek.feature.contributions.impl.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekFullScreenDialog
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.ContributionMapCanvas
import com.coffeepeek.feature.contributions.api.GeoPoint

internal typealias ContributionMapPickerSlot = @Composable (GeoPoint?, (GeoPoint, String) -> Unit, () -> Unit) -> Unit

@Composable
internal fun ContributionMapPicker(initial: GeoPoint?, location: ContributionLocation, canvas: ContributionMapCanvas,
    onConfirm: (GeoPoint, String) -> Unit, onDismiss: () -> Unit) {
    val selection = remember(initial) { ContributionMapPickerState(initial) }
    val confirm by rememberUpdatedState(onConfirm)
    LaunchedEffect(selection.state.point) { selection.resolve(location) }
    CoffeePeekFullScreenDialog(onDismiss = onDismiss) {
        ContributionMapPickerContent(selection.state, canvas, selection::move, selection::cameraApplied,
            selection::requestLocation, { selection.selection()?.let { confirm(it.first, it.second) } }, onDismiss)
    }
}
