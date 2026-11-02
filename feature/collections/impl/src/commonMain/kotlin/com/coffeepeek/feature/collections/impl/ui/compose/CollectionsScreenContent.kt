package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SharedCollection
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionsScreenContent(state: CollectionsState, onAction: (CollectionsAction) -> Unit,
    modifier: Modifier = Modifier, poster: @Composable (SharedCollection, CollectionPosterFormat, Int) -> Unit = { _, _, _ -> }) {
    CoffeePeekScaffold(modifier = modifier, topBar = {
        CoffeePeekTopBar(if (state.publicView) state.collection?.title ?: stringResource(Res.string.collections_default_title)
            else stringResource(Res.string.collections_title), stringResource(Res.string.collections_back), onBack = { onAction(CollectionsAction.Back) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            state.failure?.let { CollectionsProblem(it, onAction) }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader(stringResource(Res.string.collections_loading))
                }
                !state.publicView && !state.signedIn -> CollectionSignIn(onAction)
                state.failure == CollectionsFailure.Load && !state.hasPageContent() -> Unit
                state.page == CollectionsPage.Selection -> CollectionsSelection(state, onAction)
                state.page == CollectionsPage.Links -> CollectionsLinks(state, onAction)
                state.collection != null -> CollectionsPreview(state, onAction, poster)
            }
        }
    }
}

private fun CollectionsState.hasPageContent(): Boolean = when (page) {
    CollectionsPage.Selection -> places.isNotEmpty()
    CollectionsPage.Preview -> collection != null
    CollectionsPage.Links -> links.isNotEmpty()
}

@Composable
private fun CollectionsProblem(failure: CollectionsFailure, onAction: (CollectionsAction) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(when (failure) {
            CollectionsFailure.Load -> Res.string.collections_load_failed
            CollectionsFailure.Create -> Res.string.collections_create_failed
            CollectionsFailure.Revoke -> Res.string.collections_revoke_failed
            CollectionsFailure.Export -> Res.string.collections_export_failed
            CollectionsFailure.SelectionLimit -> Res.string.collections_limit_failed
        }), color = MaterialTheme.colorScheme.error)
        if (failure == CollectionsFailure.Load) CoffeePeekButton(stringResource(Res.string.collections_retry), { onAction(CollectionsAction.Retry) })
    }
}

@Composable
private fun CollectionSignIn(onAction: (CollectionsAction) -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.collections_sign_in_hint))
        CoffeePeekButton(stringResource(Res.string.collections_sign_in), { onAction(CollectionsAction.SignIn) })
    }
}

@Preview @Composable private fun CollectionsScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CollectionsScreenContent(CollectionsState(loading = false), {})
}
@Preview @Composable private fun CollectionsScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CollectionsScreenContent(CollectionsState(loading = false), {})
}
