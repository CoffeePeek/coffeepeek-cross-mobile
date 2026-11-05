package com.coffeepeek.feature.contributions.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.contributions.api.*
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorViewModel
import com.coffeepeek.feature.contributions.impl.ui.EditorCallbacks
import com.coffeepeek.feature.contributions.impl.ui.ChangeEditorScreen

fun createChangeEditorEntry(
    repository: ContributionsRepository, catalogs: ContributionCatalogs, account: ContributionsAccount, host: ChangeEditorHost,
): ChangeEditorEntry = ChangeEditorEntryImpl(repository, catalogs, account, host)

private class ChangeEditorEntryImpl(
    private val repository: ContributionsRepository, private val catalogs: ContributionCatalogs,
    private val account: ContributionsAccount, host: ChangeEditorHost,
) : ChangeEditorEntry {
    private val boundHost = host.forAccount(account)

    @Composable
    override fun Content(shopSlug: String, section: ChangeSection, requestId: String?, onBack: () -> Unit,
        onSignIn: () -> Unit, onOpenRequest: (String) -> Unit, modifier: Modifier) {
        val owner by account.accountKey.collectAsStateWithLifecycle(initialValue = account.currentAccountKey())
        key(owner) {
            ChangeEditorScreen(viewModel(key = "contributions-editor-$shopSlug-$section-${requestId.orEmpty()}") {
                ChangeEditorViewModel(shopSlug, section, requestId, repository, catalogs, account)
            }, editing = requestId != null, boundHost, EditorCallbacks(onBack, onSignIn, onOpenRequest), modifier)
        }
    }
}
