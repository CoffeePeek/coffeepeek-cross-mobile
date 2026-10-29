package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.layout.Arrangement
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_back
import com.coffeepeek.feature.contributions.impl.resources.contributions_load_failed
import com.coffeepeek.feature.contributions.impl.resources.contributions_loading
import com.coffeepeek.feature.contributions.impl.resources.contributions_retry
import com.coffeepeek.feature.contributions.impl.resources.contributions_sign_in
import com.coffeepeek.feature.contributions.impl.resources.contributions_sign_in_prompt
import com.coffeepeek.feature.contributions.impl.resources.contributions_tab_label
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.list.ContributionItem
import com.coffeepeek.feature.contributions.impl.list.ContributionTarget
import com.coffeepeek.feature.contributions.impl.list.ContributionsListAction
import com.coffeepeek.feature.contributions.impl.list.ContributionsListState
import com.coffeepeek.feature.contributions.impl.list.ContributionsListViewModel
import com.coffeepeek.feature.contributions.impl.list.ListError
import com.coffeepeek.feature.contributions.impl.list.TabState

internal class ListCallbacks(
    val onBack: () -> Unit,
    val onSignIn: () -> Unit,
    val onOpenShop: (String) -> Unit,
    val onOpenRequest: (String) -> Unit,
)

@Composable
internal fun ContributionsListScreen(viewModel: ContributionsListViewModel, kind: ContributionKind, callbacks: ListCallbacks, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ContributionsListContent(state, kind, callbacks, viewModel::onAction, modifier)
}

@Composable
internal fun ContributionsListContent(
    state: ContributionsListState,
    kind: ContributionKind,
    callbacks: ListCallbacks,
    onAction: (ContributionsListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(kind.title(), stringResource(Res.string.contributions_back), onBack = callbacks.onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val selected = state.selectedTab
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CoffeePeekLoader(stringResource(Res.string.contributions_loading))
                state.error == ListError.SignInRequired -> Message(stringResource(Res.string.contributions_sign_in_prompt), stringResource(Res.string.contributions_sign_in), callbacks.onSignIn)
                state.error != null -> Message(state.serverMessage ?: stringResource(Res.string.contributions_load_failed), stringResource(Res.string.contributions_retry)) { onAction(ContributionsListAction.Refresh) }
                selected == null -> Text(kind.emptyText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Tabs(state, selected, kind, callbacks, onAction)
            }
        }
    }
}

@Composable
private fun Message(text: String, action: String, onAction: () -> Unit) {
    Column(Modifier.padding(CoffeePeekDimens.spacing4), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        CoffeePeekButton(action, onAction)
    }
}

@Composable
private fun Tabs(state: ContributionsListState, selected: ModerationStatus, kind: ContributionKind, callbacks: ListCallbacks, onAction: (ContributionsListAction) -> Unit) {
    val labels = state.visibleTabs.associateWith { status ->
        stringResource(Res.string.contributions_tab_label, status.tabTitle(), state.tabs[status]?.totalCount ?: 0)
    }
    Column(Modifier.fillMaxSize()) {
        CoffeePeekCapsuleSegmentedControl(
            options = state.visibleTabs,
            selected = selected,
            label = { status -> labels.getValue(status) },
            onSelected = { onAction(ContributionsListAction.Select(it)) },
            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2),
            scrollable = true,
        )
        key(selected) {
            ContributionList(kind, selected, state.tabs[selected] ?: TabState(), callbacks, { onAction(ContributionsListAction.LoadMore(selected)) })
        }
    }
}

@Composable
private fun ContributionList(kind: ContributionKind, status: ModerationStatus, tab: TabState, callbacks: ListCallbacks, onLoadMore: () -> Unit) {
    val listState = rememberLazyListState()
    val nearEnd by remember(tab) {
        derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= tab.items.size - 3 && tab.hasMore && !tab.isLoadingMore }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onLoadMore() }
    LazyColumn(
        Modifier.fillMaxSize(), state = listState, contentPadding = coffeePeekScrollContentPadding(PaddingValues(CoffeePeekDimens.spacing4)),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        items(tab.items, key = { it.id }) { item ->
            ContributionCard(item, kind, status, { target ->
                when (target) {
                    is ContributionTarget.Shop -> callbacks.onOpenShop(target.slug)
                    is ContributionTarget.Request -> callbacks.onOpenRequest(target.id)
                }
            })
        }
    }
}

private val noCallbacks = ListCallbacks({}, {}, {}, {})

private fun previewTab(vararg items: ContributionItem) = TabState(items.toList(), items.size, 1, false)

@Preview @Composable private fun ContributionsListPreviewLight() = ContributionsListPreview(false)
@Preview @Composable private fun ContributionsListPreviewDark() = ContributionsListPreview(true)

@Composable private fun ContributionsListPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ContributionsListContent(
            ContributionsListState(
                isLoading = false,
                tabs = mapOf(
                    ModerationStatus.Approved to previewTab(
                        ContributionItem("a", title = "Sandbox Cafe", subtitle = "Тестовая улица, 1", target = ContributionTarget.Shop("cafe")),
                        ContributionItem("b", title = "Sandbox Point", subtitle = "Тестовая улица, 7"),
                    ),
                    ModerationStatus.Rejected to previewTab(ContributionItem("c", title = "Без адреса", rejectedReason = "Адрес не найден на карте")),
                ),
            ),
            ContributionKind.Shops, noCallbacks, {},
        )
    }
}

@Preview @Composable private fun ContributionsListEmptyPreviewLight() = ContributionsListEmptyPreview(false)
@Preview @Composable private fun ContributionsListEmptyPreviewDark() = ContributionsListEmptyPreview(true)

@Composable private fun ContributionsListEmptyPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { ContributionsListContent(ContributionsListState(isLoading = false), ContributionKind.Roasters, noCallbacks, {}) }
}

@Preview @Composable private fun ContributionsListErrorPreviewLight() = ContributionsListErrorPreview(false)
@Preview @Composable private fun ContributionsListErrorPreviewDark() = ContributionsListErrorPreview(true)

@Composable private fun ContributionsListErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { ContributionsListContent(ContributionsListState(isLoading = false, error = ListError.LoadFailed), ContributionKind.Changes, noCallbacks, {}) }
}
