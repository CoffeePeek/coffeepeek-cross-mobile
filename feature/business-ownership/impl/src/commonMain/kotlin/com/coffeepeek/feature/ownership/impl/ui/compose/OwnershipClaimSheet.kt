package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OwnershipClaimSheet(repository: BusinessOwnershipRepository, account: OwnershipAccount,
    kind: BusinessKind, slug: String, image: OwnershipImage, picker: OwnershipPhotoPicker, dismiss: () -> Unit, login: () -> Unit) {
    val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        val model = viewModel { OwnershipViewModel(repository, account, kind, slug) }
        val state by model.state.collectAsState()
        val currentDismiss by rememberUpdatedState(dismiss)
        LaunchedEffect(model) {
            model.events.collect {
                if (it == OwnershipEvent.Login) { currentDismiss(); login() }
                else currentDismiss()
            }
        }
        LaunchedEffect(state.generation) { if (state.generation > 1) currentDismiss() }
        OwnershipClaimSheetContent(state, model::onAction, image, picker)
    }
}

@Composable
internal fun OwnershipClaimSheetContent(state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage = { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) },
    picker: OwnershipPhotoPicker = { _, _, content -> content {} }) {
    CoffeePeekSwipeDismissModalBottomSheet({ action(OwnershipAction.Back) },
        stringResource(Res.string.ownership_back), dismissEnabled = !state.busy) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.ownership_claim_title))
            OwnershipFeedback(state, action)
            when {
                state.account == null -> {
                    Text(stringResource(Res.string.ownership_login_hint))
                    CoffeePeekButton(stringResource(Res.string.ownership_login), { action(OwnershipAction.Login) })
                }
                state.selectedClaim != null -> OwnershipClaimDetails(checkNotNull(state.selectedClaim), image)
                else -> OwnershipClaimForm(state, action, image, picker)
            }
        }
    }
    OwnershipConfirmations(state, action)
}

@org.jetbrains.compose.ui.tooling.preview.Preview
@Composable private fun OwnershipClaimSheetLightPreview() =
    com.coffeepeek.core.designsystem.theme.CoffeePeekTheme(darkTheme = false) {
        OwnershipClaimSheetContent(OwnershipState(account = "preview", destination = OwnershipDestination.Submit), {})
    }

@org.jetbrains.compose.ui.tooling.preview.Preview
@Composable private fun OwnershipClaimSheetDarkPreview() =
    com.coffeepeek.core.designsystem.theme.CoffeePeekTheme(darkTheme = true) {
        OwnershipClaimSheetContent(OwnershipState(account = "preview", destination = OwnershipDestination.Submit), {})
    }
