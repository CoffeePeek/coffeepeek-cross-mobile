package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform effects only; Contributions owns the form's visuals, photo source UI and map selection state. */
class AddShopHost(
    val locationPermission: @Composable (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit = { _, _ -> },
    val mapCanvas: ContributionMapCanvas = ContributionMapCanvas { _, _, _ -> },
    val photoPicker: ContributionPhotoPicker = previewContributionPhotoPicker,
    val feedback: (success: Boolean) -> Unit = {},
)

/** «Добавить кофейню». The caller owns navigation and the ViewModel store. */
interface AddShopEntry {
    @Composable
    fun Content(
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onAddRoaster: () -> Unit,
        modifier: Modifier,
    )
}
