package com.coffeepeek.feature.contributions.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.contributions.api.AddRoasterEntry
import com.coffeepeek.feature.contributions.api.AddRoasterHost
import com.coffeepeek.feature.contributions.api.AddShopEntry
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.api.ChangeEditorEntry
import com.coffeepeek.feature.contributions.api.ChangeEditorHost
import com.coffeepeek.feature.contributions.api.ChangeRequestEntry
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.api.ContributionsListEntry
import com.coffeepeek.feature.contributions.api.SuggestChangeEntry
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorViewModel
import com.coffeepeek.feature.contributions.impl.change.SuggestChangeViewModel
import com.coffeepeek.feature.contributions.impl.list.ContributionsListViewModel
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestViewModel
import com.coffeepeek.feature.contributions.impl.roaster.AddRoasterViewModel
import com.coffeepeek.feature.contributions.impl.shop.AddShopViewModel
import com.coffeepeek.feature.contributions.impl.ui.AddRoasterCallbacks
import com.coffeepeek.feature.contributions.impl.ui.AddRoasterScreen
import com.coffeepeek.feature.contributions.impl.ui.AddShopCallbacks
import com.coffeepeek.feature.contributions.impl.ui.AddShopScreen
import com.coffeepeek.feature.contributions.impl.ui.ChangeEditorScreen
import com.coffeepeek.feature.contributions.impl.ui.ChangeRequestScreen
import com.coffeepeek.feature.contributions.impl.ui.ContributionsListScreen
import com.coffeepeek.feature.contributions.impl.ui.EditorCallbacks
import com.coffeepeek.feature.contributions.impl.ui.ListCallbacks
import com.coffeepeek.feature.contributions.impl.ui.RequestCallbacks
import com.coffeepeek.feature.contributions.impl.ui.SuggestCallbacks
import com.coffeepeek.feature.contributions.impl.ui.SuggestChangeScreen

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

/** «Добавить обжарщика». The ViewModel lives in the destination's store; the form starts empty for every account. */
fun createAddRoasterEntry(
    repository: ContributionsRepository,
    catalogs: ContributionCatalogs,
    account: ContributionsAccount,
    host: AddRoasterHost,
): AddRoasterEntry = object : AddRoasterEntry {
    @Composable
    override fun Content(onBack: () -> Unit, onSignIn: () -> Unit, modifier: Modifier) {
        AddRoasterScreen(
            viewModel(key = "contributions-add-roaster") { AddRoasterViewModel(repository, catalogs, account) },
            host, AddRoasterCallbacks(onBack, onSignIn), modifier,
        )
    }
}

/** «Предложить правку». The ViewModel lives in the destination's store, one per shop. */
fun createSuggestChangeEntry(catalogs: ContributionCatalogs, account: ContributionsAccount): SuggestChangeEntry = object : SuggestChangeEntry {
    @Composable
    override fun Content(shopSlug: String, onBack: () -> Unit, onSignIn: () -> Unit, onPickSection: (ChangeSection) -> Unit, modifier: Modifier) {
        SuggestChangeScreen(
            viewModel(key = "contributions-suggest-$shopSlug") { SuggestChangeViewModel(shopSlug, catalogs, account) },
            SuggestCallbacks(onBack, onSignIn, onPickSection), modifier,
        )
    }
}

/** The editor of one section. The ViewModel lives in the destination's store, one per shop, section and request. */
fun createChangeEditorEntry(
    repository: ContributionsRepository,
    catalogs: ContributionCatalogs,
    account: ContributionsAccount,
    host: ChangeEditorHost,
): ChangeEditorEntry = object : ChangeEditorEntry {
    @Composable
    override fun Content(
        shopSlug: String,
        section: ChangeSection,
        requestId: String?,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenRequest: (String) -> Unit,
        modifier: Modifier,
    ) {
        ChangeEditorScreen(
            viewModel(key = "contributions-editor-$shopSlug-$section-${requestId.orEmpty()}") {
                ChangeEditorViewModel(shopSlug, section, requestId, repository, catalogs, account)
            },
            editing = requestId != null, host, EditorCallbacks(onBack, onSignIn, onOpenRequest), modifier,
        )
    }
}
