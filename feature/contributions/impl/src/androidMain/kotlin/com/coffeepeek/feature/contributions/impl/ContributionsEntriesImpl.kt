package com.coffeepeek.feature.contributions.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.contributions.api.AddShopEntry
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.api.ChangeRequestEntry
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.api.ContributionsListEntry
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.list.ContributionsListViewModel
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestViewModel
import com.coffeepeek.feature.contributions.impl.shop.AddShopViewModel
import com.coffeepeek.feature.contributions.impl.ui.AddShopCallbacks
import com.coffeepeek.feature.contributions.impl.ui.AddShopScreen
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

/** «Добавить кофейню». The ViewModel lives in the destination's store; the form starts empty for every account. */
fun createAddShopEntry(
    repository: ContributionsRepository,
    catalogs: ContributionCatalogs,
    location: ContributionLocation,
    account: ContributionsAccount,
    host: AddShopHost,
): AddShopEntry = object : AddShopEntry {
    @Composable
    override fun Content(onBack: () -> Unit, onSignIn: () -> Unit, onAddRoaster: () -> Unit, modifier: Modifier) {
        AddShopScreen(
            viewModel(key = "contributions-add-shop") { AddShopViewModel(repository, catalogs, location, account) },
            host, AddShopCallbacks(onBack, onSignIn, onAddRoaster), modifier,
        )
    }
}
