package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto

/** «Предложить правку»: the sections of a shop that can be changed. The caller owns navigation and the ViewModel store. */
interface SuggestChangeEntry {
    @Composable
    fun Content(
        shopSlug: String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onPickSection: (ChangeSection) -> Unit,
        modifier: Modifier,
    )
}

/**
 * What the change editor borrows from the application: the photo picker, remote images of the saved photos, the brew method icons, the roaster
 * logos and the success/failure feedback. Defaults keep previews and tests free of app services.
 */
class ChangeEditorHost(
    /** Picks photos (several at once), shows the chosen ones with a remove action and the title and hint. */
    val photos: @Composable (photos: List<PendingPhoto>, max: Int, title: String, hint: String, onAdd: (List<PendingPhoto>) -> Unit, onRemove: (Int) -> Unit) -> Unit =
        { _, _, _, _, _, _ -> },
    /** A saved photo of the shop by its address. */
    val remoteImage: @Composable (url: String, modifier: Modifier) -> Unit = { _, _ -> },
    val brewMethodIcon: @Composable (name: String) -> Unit = {},
    val roasterLogo: @Composable (roaster: CatalogOption) -> Unit = {},
    /** Called once when the editor ends: true for a sent request, false for a failed one. */
    val feedback: (success: Boolean) -> Unit = {},
)

/** The editor of one section; a [requestId] edits an own request that is not approved yet. */
interface ChangeEditorEntry {
    @Composable
    fun Content(
        shopSlug: String,
        section: ChangeSection,
        requestId: String?,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenRequest: (id: String) -> Unit,
        modifier: Modifier,
    )
}
