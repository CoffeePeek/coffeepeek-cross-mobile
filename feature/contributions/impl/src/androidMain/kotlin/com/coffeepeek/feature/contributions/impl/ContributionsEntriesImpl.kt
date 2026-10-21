package com.coffeepeek.feature.contributions.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.contributions.api.ChangeRequestEntry
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.api.ContributionsListEntry
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.list.ContributionsListViewModel
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestViewModel
import com.coffeepeek.feature.contributions.impl.ui.ChangeRequestScreen
import com.coffeepeek.feature.contributions.impl.ui.ContributionsListScreen
import com.coffeepeek.feature.contributions.impl.ui.ListCallbacks
import com.coffeepeek.feature.contributions.impl.ui.RequestCallbacks

/** The lists of sent shops, roasters and changes. The ViewModel lives in the destination's store, one per kind. */
fun createContributionsListEntry(repository: ContributionsRepository, account: ContributionsAccount): ContributionsListEntry = object : ContributionsListEntry {
    @Composable
    override fun Content(
        kind: ContributionKind,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit,
        onOpenRequest: (String) -> Unit,
        modifier: Modifier,
    ) {
        ContributionsListScreen(
            viewModel(key = "contributions-$kind") { ContributionsListViewModel(kind, repository, account) },
            kind, ListCallbacks(onBack, onSignIn, onOpenShop, onOpenRequest), modifier,
        )
    }
}

/** One sent change request. The ViewModel lives in the destination's store, one per request. */
fun createChangeRequestEntry(repository: ContributionsRepository, account: ContributionsAccount): ChangeRequestEntry = object : ChangeRequestEntry {
    @Composable
    override fun Content(requestId: String, onBack: () -> Unit, onSignIn: () -> Unit, onOpenShop: (String) -> Unit, modifier: Modifier) {
        ChangeRequestScreen(
            viewModel(key = "contribution-request-$requestId") { ChangeRequestViewModel(requestId, repository, account) },
            RequestCallbacks(onBack, onSignIn, onOpenShop), modifier,
        )
    }
}
