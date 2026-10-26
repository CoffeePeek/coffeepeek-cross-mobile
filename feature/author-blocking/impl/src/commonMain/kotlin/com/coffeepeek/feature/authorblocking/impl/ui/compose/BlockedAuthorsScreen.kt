package com.coffeepeek.feature.authorblocking.impl.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.feature.authorblocking.impl.resources.*
import com.coffeepeek.feature.authorblocking.impl.ui.compose.component.BlockedAuthorRow
import com.coffeepeek.feature.authorblocking.impl.ui.compose.component.BlockedAuthorsGuestCard
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BlockedAuthorsScreen(presenter: AuthorBlockingPresenter, onBack: () -> Unit,
    onLogin: () -> Unit, onRegister: () -> Unit) {
    val state by presenter.blocks.collectAsStateWithLifecycle()
    val prompt by presenter.prompt.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.refresh() }
    BlockedAuthorsScreenContent(state, prompt?.working == true, onBack, onLogin, onRegister,
        presenter::refresh, presenter::requestUnblock)
}

@Composable
internal fun BlockedAuthorsScreenContent(state: AuthorBlockingListState, working: Boolean,
    onBack: () -> Unit, onLogin: () -> Unit, onRegister: () -> Unit, onRefresh: () -> Unit,
    onUnblock: (AuthorReference) -> Unit) {
    CoffeePeekBackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CoffeePeekTopBar(stringResource(Res.string.author_blocking_title),
            stringResource(Res.string.author_blocking_back), onBack = onBack)
        LazyColumn(Modifier.fillMaxSize().coffeePeekScreenBottomPadding(), contentPadding = PaddingValues(
            horizontal = CoffeePeekDimens.settingsPagePadding, vertical = CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            item { BlockedAuthorsHeading() }
            if (!state.signedIn) item { BlockedAuthorsGuestCard(onLogin, onRegister) }
            else authorListItems(state, working, onRefresh, onUnblock)
        }
    }
}

private fun LazyListScope.authorListItems(state: AuthorBlockingListState, working: Boolean,
    onRefresh: () -> Unit, onUnblock: (AuthorReference) -> Unit) {
    if (state.loading) item {
        Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing8), contentAlignment = Alignment.Center) {
            CoffeePeekLoader(stringResource(Res.string.author_blocking_loading))
        }
    }
    state.error?.let { error -> item { BlockedAuthorsError(error, onRefresh) } }
    if (state.ready && !state.loading && state.error == null && state.items.isEmpty()) item {
        CoffeePeekEmptyState(stringResource(Res.string.author_blocking_empty_title),
            stringResource(Res.string.author_blocking_empty_message), CoffeePeekIcons.VisibilityOff,
            Modifier.padding(top = CoffeePeekDimens.spacing6))
    }
    items(state.items, key = { it.id }) { BlockedAuthorRow(it, !working, onUnblock) }
    item { Spacer(Modifier.coffeePeekViewportBottomPadding().height(CoffeePeekDimens.spacing4)) }
}

@Composable
private fun BlockedAuthorsHeading() {
    val colors = MaterialTheme.colorScheme
    BasicText(stringResource(Res.string.author_blocking_heading), Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineSmall.copy(color = colors.onBackground))
    Spacer(Modifier.height(CoffeePeekDimens.spacing2))
    BasicText(stringResource(Res.string.author_blocking_description),
        style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant))
    Spacer(Modifier.height(CoffeePeekDimens.spacing2))
}

@Composable
private fun BlockedAuthorsError(message: String, onRefresh: () -> Unit) {
    CoffeePeekCard(shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = 0.dp) {
        BasicText(stringResource(Res.string.author_blocking_list_failed), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        BasicText(message, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        CoffeePeekTextAction(stringResource(Res.string.author_blocking_retry), onRefresh,
            Modifier.align(Alignment.Start), emphasized = true)
    }
}

@Composable private fun BlockedAuthorsPreview(dark: Boolean, guest: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    BlockedAuthorsScreenContent(AuthorBlockingListState(signedIn = !guest, ready = true,
        items = if (guest) emptyList() else listOf(BlockedAuthorItem("a", "Анна", "anna", null))),
        false, {}, {}, {}, {}, {})
}
@Preview @Composable private fun BlockedAuthorsLightPreview() = BlockedAuthorsPreview(false)
@Preview @Composable private fun BlockedAuthorsDarkPreview() = BlockedAuthorsPreview(true)
@Preview @Composable private fun BlockedAuthorsGuestLightPreview() = BlockedAuthorsPreview(false, true)
@Preview @Composable private fun BlockedAuthorsGuestDarkPreview() = BlockedAuthorsPreview(true, true)
