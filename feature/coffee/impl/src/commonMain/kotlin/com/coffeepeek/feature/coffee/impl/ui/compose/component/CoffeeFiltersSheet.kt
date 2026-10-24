package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.coffee.impl.resources.Res
import com.coffeepeek.feature.coffee.impl.resources.coffee_available_only
import com.coffeepeek.feature.coffee.impl.resources.coffee_close_filters
import com.coffeepeek.feature.coffee.impl.resources.coffee_done
import com.coffeepeek.feature.coffee.impl.resources.coffee_filters
import com.coffeepeek.feature.coffee.impl.resources.coffee_filters_failed
import com.coffeepeek.feature.coffee.impl.resources.coffee_loading
import com.coffeepeek.feature.coffee.impl.resources.coffee_reset
import com.coffeepeek.feature.coffee.impl.resources.coffee_retry
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListAction
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListState

@Composable
internal fun CoffeeFiltersSheet(state: CoffeeListState, onAction: (CoffeeListAction) -> Unit) {
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = { onAction(CoffeeListAction.CancelFilters) },
        dismissDescription = stringResource(Res.string.coffee_close_filters),
    ) { CoffeeFiltersContent(state, onAction) }
}

/** Draft belongs to MVI, so apply/cancel do not accidentally mutate active results. */
@Composable
internal fun CoffeeFiltersContent(state: CoffeeListState, onAction: (CoffeeListAction) -> Unit) {
    val draft = state.filterDraft ?: return
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing1), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ onAction(CoffeeListAction.ResetFilterDraft) }, enabled = draft.activeCount > 0) { Text(stringResource(Res.string.coffee_reset)) }
            Text(stringResource(Res.string.coffee_filters), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton({ onAction(CoffeeListAction.ApplyFilters) }) { Text(stringResource(Res.string.coffee_done)) }
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
            CoffeePeekGroupSection {
                CoffeePeekSwitchRow(stringResource(Res.string.coffee_available_only), draft.availableOnly) {
                    onAction(CoffeeListAction.AvailableOnlyChanged(it))
                }
            }
            when {
                state.filtersLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader(stringResource(Res.string.coffee_loading))
                }
                state.filtersFailed -> Column {
                    Text(stringResource(Res.string.coffee_filters_failed))
                    TextButton({ onAction(CoffeeListAction.RetryFilters) }) { Text(stringResource(Res.string.coffee_retry)) }
                }
                else -> state.filterGroups.forEach { group ->
                    CoffeePeekGroupSection(group.name) {
                        group.options.forEachIndexed { index, option ->
                            if (index > 0) CoffeePeekRowSeparator()
                            CoffeePeekCheckmarkRow(option.name, option.code in draft.values[group.code].orEmpty(), {
                                onAction(CoffeeListAction.FilterToggled(group.code, option.code))
                            })
                        }
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun CoffeeFiltersPreview() = CoffeePeekTheme {
    Surface(Modifier.height(400.dp)) {
        CoffeeFiltersContent(CoffeeListState(filterDraft = CoffeeFilters(), filtersLoading = false), {})
    }
}
