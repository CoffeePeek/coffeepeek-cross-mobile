package com.coffeepeek.feature.ownership.impl

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.OwnershipAccess
import com.coffeepeek.feature.ownership.impl.ui.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import com.coffeepeek.feature.ownership.impl.ui.compose.BusinessOwnershipScreen
import com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipEvent

fun createBusinessOwnershipEntry(
    repository: BusinessOwnershipRepository, account: OwnershipAccount,
    image: OwnershipImage, picker: OwnershipPhotoPicker, backHandler: OwnershipBackHandler,
    onEdit: (String, String) -> Unit = { _, _ -> },
    onOpenBusiness: (String, String) -> Unit = { _, _ -> },
    onLogin: () -> Unit = {},
): BusinessOwnershipEntry = OwnershipEntry(repository, account, image, picker, backHandler, onEdit, onOpenBusiness, onLogin)

private class OwnershipEntry(
    private val repository: BusinessOwnershipRepository, private val account: OwnershipAccount,
    private val image: OwnershipImage, private val picker: OwnershipPhotoPicker, private val backHandler: OwnershipBackHandler,
    private val onEdit: (String, String) -> Unit,
    private val onOpenBusiness: (String, String) -> Unit,
    private val onLogin: () -> Unit,
) : BusinessOwnershipEntry {
    private val access = OwnershipAccess(repository, account)
    private val profileChanges = kotlinx.coroutines.flow.MutableStateFlow(0L)
    @Composable override fun ProfileChanges(): Long = profileChanges.collectAsState().value
    @Composable override fun Session(): BusinessSessionState {
        LaunchedEffect(access) { account.observe().distinctUntilChanged().collect {
            access.reset()
            access.refresh()
        } }
        val scope = rememberCoroutineScope()
        androidx.lifecycle.compose.LifecycleResumeEffect(access) {
            val job = scope.launch { access.refresh() }
            onPauseOrDispose { job.cancel() }
        }
        return access.state.collectAsState().value
    }
    @Composable override fun AccountSwitch() {
        val state by access.state.collectAsState()
        val scope = rememberCoroutineScope()
        OwnershipAccountSwitch(state) { scope.launch { access.switchMode() } }
    }
    @Composable override fun Tab(onCheckIns: () -> Unit, checkIns: @Composable () -> Unit) {
        val model = viewModel(key = "business-tab") { OwnershipViewModel(repository, account, businessTab = true) }
        val state by model.state.collectAsState()
        BusinessTabContent(state, model::onAction, onCheckIns, checkIns, edit = { kind, id -> onEdit(kind.name, id) }) { kind, id ->
            val business = (state.shops + state.roasters?.items.orEmpty()).firstOrNull { it.kind == kind && it.id == id }
            val slug = business?.slug
            if (slug.isNullOrBlank()) onEdit(kind.name, id) else onOpenBusiness(kind.name, slug)
        }
    }
    @Composable override fun ProfileActions(kind: String, id: String, slug: String, onSuggest: () -> Unit, onOwnerEdit: () -> Unit,
        favorite: @Composable () -> Unit, share: @Composable () -> Unit) {
        val state by access.state.collectAsState()
        val businessKind = BusinessKind.valueOf(kind)
        val editorId = if ("$kind:$id" in state.ownedIdentities || "$kind:$slug" in state.ownedIdentities) access.editorId(businessKind, id, slug) else null
        OwnershipProfileActions(editorId != null, onSuggest, { editorId?.let { onOwnerEdit(); onEdit(kind, it) } }, favorite, share, businessKind == BusinessKind.Roaster) { dismiss ->
            OwnershipClaimSheet(repository, account, businessKind, slug, image, picker, dismiss, onLogin)
        }
    }

    @Composable
    override fun Content(onBack: () -> Unit, onLogin: () -> Unit, initialKind: String?, initialSlug: String?, initialId: String?) {
        val model = viewModel(key = "ownership:$initialKind:$initialSlug:$initialId") {
            OwnershipViewModel(repository, account, BusinessKind.entries.find { it.name == initialKind }, initialSlug, initialId)
        }
        val back by rememberUpdatedState(onBack)
        val login by rememberUpdatedState(onLogin)
        LaunchedEffect(model) {
            model.events.collect {
                if (it == OwnershipEvent.Close) {
                    if (model.state.value.saved) profileChanges.value++
                    back()
                } else login()
            }
        }
        backHandler { model.onAction(com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipAction.Back) }
        BusinessOwnershipScreen(model, image, picker)
    }
}
