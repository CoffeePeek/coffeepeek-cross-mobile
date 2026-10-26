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
import com.coffeepeek.feature.contributions.impl.roaster.AddRoasterViewModel
import com.coffeepeek.feature.contributions.impl.ui.AddRoasterCallbacks
import com.coffeepeek.feature.contributions.impl.ui.AddRoasterScreen

fun createAddRoasterEntry(
    repository: ContributionsRepository, catalogs: ContributionCatalogs, account: ContributionsAccount, host: AddRoasterHost,
): AddRoasterEntry = AddRoasterEntryImpl(repository, catalogs, account, host)

private class AddRoasterEntryImpl(
    private val repository: ContributionsRepository, private val catalogs: ContributionCatalogs,
    private val account: ContributionsAccount, host: AddRoasterHost,
) : AddRoasterEntry {
    private val boundHost = host.forAccount(account)

    @Composable
    override fun Content(onBack: () -> Unit, onSignIn: () -> Unit, modifier: Modifier) {
        val owner by account.accountKey.collectAsStateWithLifecycle(initialValue = account.currentAccountKey())
        key(owner) {
            AddRoasterScreen(viewModel(key = "contributions-add-roaster") { AddRoasterViewModel(repository, catalogs, account) },
                boundHost, AddRoasterCallbacks(onBack, onSignIn), modifier)
        }
    }
}
