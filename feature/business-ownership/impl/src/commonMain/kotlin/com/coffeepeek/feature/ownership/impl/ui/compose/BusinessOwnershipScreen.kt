package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun BusinessOwnershipScreen(model: OwnershipViewModel, image: OwnershipImage, picker: OwnershipPhotoPicker) {
    val state by model.state.collectAsStateWithLifecycle()
    androidx.compose.ui.backhandler.BackHandler { model.onAction(OwnershipAction.Back) }
    BusinessOwnershipScreenContent(state, model::onAction, image, picker)
}

@Composable
internal fun BusinessOwnershipScreenContent(
    state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage = { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) },
    picker: OwnershipPhotoPicker = { _, _, content -> content {} },
) {
    CoffeePeekScaffold(topBar = {
        CoffeePeekTopBar(title = stringResource(when (state.destination) {
            OwnershipDestination.Overview -> Res.string.ownership_title
            OwnershipDestination.Submit, OwnershipDestination.Claim -> Res.string.ownership_claim_title
            OwnershipDestination.Editor -> Res.string.ownership_editor_title
        }), backDescription = stringResource(Res.string.ownership_back), onBack = { action(OwnershipAction.Back) })
    }) { padding ->
        if (state.destination == OwnershipDestination.Overview) {
            OwnershipOverviewScrollable(state, action, image, picker, Modifier.padding(padding))
        } else {
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(16.dp).coffeePeekScreenBottomPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OwnershipBody(state, action, image, picker)
            }
        }
    }
    OwnershipConfirmations(state, action)
}

@Composable
private fun OwnershipOverviewScrollable(state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage, picker: OwnershipPhotoPicker, modifier: Modifier) {
    val listState = rememberLazyListState()
    CoffeePeekPullToRefresh(listState, state.loading, { action(OwnershipAction.Refresh) },
        stringResource(Res.string.ownership_refresh), stringResource(Res.string.ownership_loading),
        modifier = modifier.fillMaxSize()) { scrollModifier ->
        LazyColumn(Modifier.fillMaxSize().then(scrollModifier), state = listState) {
            item {
                Column(Modifier.padding(16.dp).coffeePeekScreenBottomPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OwnershipBody(state, action, image, picker)
                }
            }
        }
    }
}

@Composable
private fun OwnershipBody(state: OwnershipState, action: (OwnershipAction) -> Unit, image: OwnershipImage, picker: OwnershipPhotoPicker) {
    if (state.account == null || state.problem == OwnershipProblem.SignedOut) {
        Text(stringResource(Res.string.ownership_login_hint))
        CoffeePeekButton(stringResource(Res.string.ownership_login), { action(OwnershipAction.Login) })
        return
    }
    OwnershipFeedback(state, action)
    if (state.loading) { CoffeePeekLoader(contentDescription = stringResource(Res.string.ownership_loading))
    return }
    when (state.destination) {
        OwnershipDestination.Overview -> OwnershipOverview(state, action)
        OwnershipDestination.Submit -> OwnershipClaimForm(state, action, image, picker)
        OwnershipDestination.Claim -> state.selectedClaim?.let { OwnershipClaimDetails(it, image) }
        OwnershipDestination.Editor -> state.profile?.let { BusinessProfileEditor(state, action, image, picker) }
    }
}

@Composable
internal fun OwnershipConfirmations(state: OwnershipState, action: (OwnershipAction) -> Unit) {
    val cancel = stringResource(Res.string.ownership_cancel)
    if (state.confirmPublish) CoffeePeekActionDialog(stringResource(Res.string.ownership_publish_title),
        stringResource(Res.string.ownership_publish_hint),
        listOf(CoffeePeekDialogAction(stringResource(Res.string.ownership_save), { action(OwnershipAction.Save) }, enabled = !state.busy)),
        cancel, { action(OwnershipAction.CancelConfirmation) })
    if (state.confirmDiscard) CoffeePeekActionDialog(stringResource(Res.string.ownership_discard_title),
        stringResource(Res.string.ownership_discard_hint),
        listOf(CoffeePeekDialogAction(stringResource(Res.string.ownership_discard), {
            action(if (state.reloadAfterDiscard) OwnershipAction.Reload else OwnershipAction.Discard)
        }, destructive = true)), cancel, { action(OwnershipAction.CancelConfirmation) })
}

@Preview @Composable private fun BusinessOwnershipLightPreview() = CoffeePeekTheme(darkTheme = false) {
    BusinessOwnershipScreenContent(OwnershipState(account = "preview", destination = OwnershipDestination.Submit), {})
}
@Preview @Composable private fun BusinessOwnershipDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    BusinessOwnershipScreenContent(OwnershipState(account = "preview", destination = OwnershipDestination.Submit), {})
}
