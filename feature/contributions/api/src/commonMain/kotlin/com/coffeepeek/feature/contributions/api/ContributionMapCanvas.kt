package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Native map surface only, with callbacks on the UI thread. The feature owns dialog, controls and address selection. */
fun interface ContributionMapCanvas {
    @Composable fun Content(state: ContributionMapCanvasState, callbacks: ContributionMapCallbacks, modifier: Modifier)
}

data class ContributionMapCanvasState(
    val cameraTarget: GeoPoint?, val locationRequest: Int, val isDarkTheme: Boolean, val zoom: Float = 16f,
)

class ContributionMapCallbacks(
    val onCameraApplied: () -> Unit,
    val onCenterChanged: (GeoPoint) -> Unit,
    val onMyLocationFound: (GeoPoint) -> Unit,
)
