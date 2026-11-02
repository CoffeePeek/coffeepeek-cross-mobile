package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.CollectionSummary
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionsLinks(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    var pendingId by remember { mutableStateOf<String?>(null) }
    val pending = state.links.firstOrNull { it.id == pendingId }
    if (pending != null) CoffeePeekDestructiveConfirmationSheet(
        stringResource(Res.string.collections_revoke_title), stringResource(Res.string.collections_revoke_message),
        stringResource(Res.string.collections_revoke), stringResource(Res.string.collections_cancel),
        onConfirm = { pendingId = null; onAction(CollectionsAction.Revoke(pending.id)) }, onDismiss = { pendingId = null })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stringResource(Res.string.collections_links), style = MaterialTheme.typography.headlineSmall) }
        if (state.links.isEmpty()) item { Text(stringResource(Res.string.collections_no_links)) }
        items(state.links, key = { it.id }) { link -> CollectionLink(link, state.busy,
            { onAction(CollectionsAction.OpenCollection(link.id)) }, { pendingId = link.id }) }
    }
}

@Composable
private fun CollectionLink(link: CollectionSummary, busy: Boolean, onOpen: () -> Unit, onRevoke: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(link.title, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.collections_count, link.itemCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekButton(stringResource(Res.string.collections_open), onOpen, enabled = !busy)
        CoffeePeekTextButton(onClick = onRevoke, enabled = !busy) { Text(stringResource(Res.string.collections_revoke)) }
    }
}

@Composable
private fun CollectionsLinksPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CollectionsLinks(CollectionsState(signedIn = true, loading = false, page = CollectionsPage.Links,
        links = listOf(CollectionSummary("sample", "Кофейный Минск", "https://coffeepeek.by", 8))), {})
}
@Preview @Composable private fun CollectionsLinksLightPreview() = CollectionsLinksPreview(false)
@Preview @Composable private fun CollectionsLinksDarkPreview() = CollectionsLinksPreview(true)
