package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform photo launchers and success/failure feedback; the feature owns their UI. */
class AddRoasterHost(
    val photoPicker: ContributionPhotoPicker = previewContributionPhotoPicker,
    val feedback: (success: Boolean) -> Unit = {},
)

/** «Добавить обжарщика». The caller owns navigation and the ViewModel store. */
interface AddRoasterEntry {
    @Composable
    fun Content(
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        modifier: Modifier,
    )
}
