package com.coffeepeek.feature.ownership.impl

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.BusinessOwnershipScreen
import com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipEvent
import kotlin.uuid.Uuid

fun createBusinessOwnershipEntry(
    repository: BusinessOwnershipRepository, account: OwnershipAccount,
    image: OwnershipImage, picker: OwnershipPhotoPicker, backHandler: OwnershipBackHandler,
): BusinessOwnershipEntry = OwnershipEntry(repository, account, image, picker, backHandler)

private class OwnershipEntry(
    private val repository: BusinessOwnershipRepository, private val account: OwnershipAccount,
    private val image: OwnershipImage, private val picker: OwnershipPhotoPicker, private val backHandler: OwnershipBackHandler,
) : BusinessOwnershipEntry {
    @Composable
    override fun Content(onBack: () -> Unit, onLogin: () -> Unit, initialKind: String?, initialSlug: String?) {
        val model = viewModel(key = "ownership:$initialKind:$initialSlug") {
            OwnershipViewModel(repository, account, BusinessKind.entries.find { it.name == initialKind }, initialSlug)
        }
        val back by rememberUpdatedState(onBack)
        val login by rememberUpdatedState(onLogin)
        LaunchedEffect(model) { model.events.collect { if (it == OwnershipEvent.Close) back() else login() } }
        backHandler { model.onAction(com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipAction.Back) }
        BusinessOwnershipScreen(model, image, picker)
    }
}
