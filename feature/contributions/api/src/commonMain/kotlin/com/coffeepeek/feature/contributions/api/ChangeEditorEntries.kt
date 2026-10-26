package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.contributions.domain.model.ChangeSection

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

/** Platform photo launchers and success/failure feedback; the feature owns their UI. */
class ChangeEditorHost(
    val photoPicker: ContributionPhotoPicker = previewContributionPhotoPicker,
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
