package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto

/** What the roaster form borrows from the application: the photo picker and the success/failure feedback. Defaults keep previews and tests free of app services. */
class AddRoasterHost(
    /** Picks photos (several at once), shows the chosen ones with a remove action and the title and hint. */
    val photos: @Composable (photos: List<PendingPhoto>, max: Int, title: String, hint: String, onAdd: (List<PendingPhoto>) -> Unit, onRemove: (Int) -> Unit) -> Unit =
        { _, _, _, _, _, _ -> },
    /** Called once when the form ends: true for a sent roaster, false for a failed one. */
    val feedback: (success: Boolean) -> Unit = {},
)

/** «Добавить обжарщика». The caller owns navigation and the ViewModel store. */
interface AddRoasterEntry {
    @Composable
    fun Content(
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        modifier: Modifier = Modifier,
    )
}
