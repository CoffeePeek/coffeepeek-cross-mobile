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
import com.coffeepeek.feature.contributions.impl.shop.AddShopViewModel
import com.coffeepeek.feature.contributions.impl.ui.AddShopCallbacks
import com.coffeepeek.feature.contributions.impl.ui.AddShopScreen

fun createAddShopEntry(
    repository: ContributionsRepository, catalogs: ContributionCatalogs, location: ContributionLocation,
    account: ContributionsAccount, host: AddShopHost,
): AddShopEntry = AddShopEntryImpl(repository, catalogs, location, account, host)

private class AddShopEntryImpl(
    private val repository: ContributionsRepository, private val catalogs: ContributionCatalogs,
    private val location: ContributionLocation, private val account: ContributionsAccount, host: AddShopHost,
) : AddShopEntry {
    private val boundHost = host.forAccount(account)

    @Composable
    override fun Content(onBack: () -> Unit, onSignIn: () -> Unit, onAddRoaster: () -> Unit, modifier: Modifier) {
        val owner by account.accountKey.collectAsStateWithLifecycle(initialValue = account.currentAccountKey())
        key(owner) {
            AddShopScreen(viewModel(key = "contributions-add-shop") { AddShopViewModel(repository, catalogs, location, account) },
                boundHost, AddShopCallbacks(onBack, onSignIn, onAddRoaster), location, modifier)
        }
    }
}
