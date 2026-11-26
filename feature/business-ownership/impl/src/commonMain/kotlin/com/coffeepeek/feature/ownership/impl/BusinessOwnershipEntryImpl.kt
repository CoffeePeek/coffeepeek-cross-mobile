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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.coffeepeek.feature.ownership.impl.ui.compose.BusinessOwnershipScreen
import com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipEvent

fun createBusinessOwnershipEntry(
    repository: BusinessOwnershipRepository, account: OwnershipAccount,
    image: OwnershipImage, picker: OwnershipPhotoPicker, backHandler: OwnershipBackHandler,
    checkInLink: (String) -> String?, shareCheckInQr: (ByteArray, String) -> Unit,
    onEdit: (String, String) -> Unit = { _, _ -> },
    onOpenBusiness: (String, String) -> Unit = { _, _ -> },
    onLogin: () -> Unit = {},
): BusinessOwnershipEntry {
    return OwnershipEntry(repository, account, image, picker, backHandler, checkInLink, shareCheckInQr,
        onEdit, onOpenBusiness, onLogin)
}

internal class OwnershipEntry(
    private val repository: BusinessOwnershipRepository, private val account: OwnershipAccount,
    private val image: OwnershipImage, private val picker: OwnershipPhotoPicker, private val backHandler: OwnershipBackHandler,
    private val checkInLink: (String) -> String?,
    private val shareCheckInQr: (ByteArray, String) -> Unit,
    private val onEdit: (String, String) -> Unit,
    private val onOpenBusiness: (String, String) -> Unit,
    private val onLogin: () -> Unit,
) : BusinessOwnershipEntry {
    private val access = OwnershipAccess(repository, account)
    private val mutableProfileChanges = kotlinx.coroutines.flow.MutableStateFlow(0L)
    override val profileChanges = mutableProfileChanges.asStateFlow()
    internal fun nativeSignedIn() = account.current() != null
    internal fun nativeModel(tab: Boolean, kind: BusinessKind?, slug: String?, id: String?) =
        OwnershipViewModel(repository, account, kind, slug, id, tab)
    internal fun nativeSession(model: OwnershipViewModel, tab: Boolean, claim: Boolean,
        choosePhotos: () -> Unit, dispose: () -> Unit) =
        IosOwnershipSession(model, account, tab, claim, choosePhotos, checkInLink, shareCheckInQr,
            { kind, id -> openBusiness(model.state.value, kind, id) }, dispose)
    internal fun profileEditorId(kind: BusinessKind, id: String, slug: String,
        identities: Set<String> = access.state.value.ownedIdentities): String? {
        if ("${kind.name}:$id" !in identities && "${kind.name}:$slug" !in identities) return null
        return access.editorId(kind, id, slug)
    }
    internal fun nativeEdit(kind: String, id: String) = onEdit(kind, id)
    internal fun nativeLogin() = onLogin()
    internal fun nativeProfileSaved() { mutableProfileChanges.value++ }
    @Composable override fun ProfileChanges(): Long = profileChanges.collectAsState().value
    override suspend fun observeSessionChanges() {
        account.observe().distinctUntilChanged().collect {
            access.reset()
            access.refresh()
        }
    }
    override suspend fun refreshSession() = access.refresh()
    @Composable override fun Session(): BusinessSessionState {
        LaunchedEffect(access) { observeSessionChanges() }
        val scope = rememberCoroutineScope()
        androidx.lifecycle.compose.LifecycleResumeEffect(access) {
            val job = scope.launch { refreshSession() }
            onPauseOrDispose { job.cancel() }
        }
        return access.state.collectAsState().value
    }
    override val accountSwitchState = access.state.asStateFlow()

    override suspend fun switchAccountMode() = access.switchMode()

    @Composable override fun AccountSwitch() {
        val state by accountSwitchState.collectAsState()
        val scope = rememberCoroutineScope()
        OwnershipAccountSwitch(state) { scope.launch { switchAccountMode() } }
    }
    @Composable override fun Tab() {
        val model = viewModel(key = "business-tab") { OwnershipViewModel(repository, account, businessTab = true) }
        val state by model.state.collectAsState()
        val open = { kind: String, id: String -> openBusiness(state, kind, id) }
        if (PlatformOwnershipContent(model, account, picker, tab = true,
                checkInLink = checkInLink, shareQr = shareCheckInQr, openBusiness = open)) return
        BusinessTabContent(state, model::onAction, image, checkInLink, shareCheckInQr) { kind, id -> open(kind.name, id) }
    }

    private fun openBusiness(state: com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipState, kind: String, id: String) {
        val business = (state.shops + state.roasters?.items.orEmpty()).firstOrNull { it.kind.name == kind && it.id == id } ?: return
        val slug = business.slug
        if (slug.isNullOrBlank()) onEdit(kind, id) else onOpenBusiness(kind, slug)
    }

    @Composable override fun ProfileActions(kind: String, id: String, slug: String, onSuggest: () -> Unit, onOwnerEdit: () -> Unit,
        favorite: @Composable () -> Unit, share: @Composable () -> Unit) {
        val state by access.state.collectAsState()
        val businessKind = BusinessKind.valueOf(kind)
        val editorId = profileEditorId(businessKind, id, slug, state.ownedIdentities)
        OwnershipProfileActions(
            owner = editorId != null,
            suggest = onSuggest,
            edit = { editorId?.let { onOwnerEdit(); onEdit(kind, it) } },
            favorite = favorite,
            share = share,
            isSignedIn = { account.current() != null },
            onLogin = onLogin,
            roaster = businessKind == BusinessKind.Roaster,
        ) { dismiss ->
            ClaimContent(businessKind, slug, dismiss, onLogin)
        }
    }

    @Composable
    internal fun ClaimContent(kind: BusinessKind, slug: String, dismiss: () -> Unit, login: () -> Unit, nativeModal: Boolean = true) {
        OwnershipClaimSheet(repository, account, kind, slug, image, picker, dismiss, login, nativeModal)
    }

    @Composable
    override fun Content(onBack: () -> Unit, onLogin: () -> Unit, initialKind: String?, initialSlug: String?, initialId: String?) {
        val model = viewModel(key = "ownership:$initialKind:$initialSlug:$initialId") {
            OwnershipViewModel(repository, account, BusinessKind.entries.find { it.name == initialKind }, initialSlug, initialId)
        }
        val back by rememberUpdatedState(onBack)
        val login by rememberUpdatedState(onLogin)
        LaunchedEffect(model) {
            model.events.collect { event ->
                when (event) {
                    OwnershipEvent.Close -> back()
                    OwnershipEvent.Login -> login()
                    OwnershipEvent.ProfileSaved -> nativeProfileSaved()
                }
            }
        }
        backHandler { model.onAction(com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipAction.Back) }
        if (!PlatformOwnershipContent(model, account, picker)) BusinessOwnershipScreen(model, image, picker)
    }
}
