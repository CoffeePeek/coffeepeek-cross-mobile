package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterGroup
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterOption
import com.coffeepeek.feature.coffee.impl.resources.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListAction
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListState

@Composable
internal fun CoffeeFiltersSheet(state: CoffeeListState, onAction: (CoffeeListAction) -> Unit) {
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = { onAction(CoffeeListAction.CancelFilters) },
        dismissDescription = stringResource(Res.string.coffee_close_filters),
        maxSheetHeightFraction = 0.8f,
        expandToMaxHeight = true,
    ) { CoffeeFiltersContent(state, onAction) }
}

/** Draft belongs to MVI, so apply/cancel do not accidentally mutate active results. */
@Composable
internal fun CoffeeFiltersContent(state: CoffeeListState, onAction: (CoffeeListAction) -> Unit) {
    val draft = state.filterDraft ?: return
    Column(Modifier.fillMaxWidth()) {
        CoffeeFiltersHeader(draft.activeCount > 0, onAction)
        CoffeeFilterOptions(state, draft, onAction)
    }
}

@Composable
private fun CoffeeFiltersHeader(resetEnabled: Boolean, onAction: (CoffeeListAction) -> Unit) {
    val reset: @Composable () -> Unit = {
        TextButton({ onAction(CoffeeListAction.ResetFilterDraft) }, enabled = resetEnabled) {
            Text(stringResource(Res.string.coffee_reset))
        }
    }
    val done: @Composable () -> Unit = {
        TextButton({ onAction(CoffeeListAction.ApplyFilters) }) { Text(stringResource(Res.string.coffee_done)) }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing1)) {
        if (LocalDensity.current.fontScale > 1.2f) {
            Text(stringResource(Res.string.coffee_filters), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                reset()
                done()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                reset()
                Text(stringResource(Res.string.coffee_filters), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                done()
            }
        }
    }
}

@Composable
private fun ColumnScope.CoffeeFilterOptions(
    state: CoffeeListState, draft: CoffeeFilters, onAction: (CoffeeListAction) -> Unit,
) {
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
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
            else -> CoffeeFilterGroups(state.filterGroups, draft, onAction)
        }
    }
}

@Composable
private fun CoffeeFilterGroups(groups: List<CoffeeFilterGroup>, draft: CoffeeFilters, onAction: (CoffeeListAction) -> Unit) {
    groups.forEach { group ->
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

@Composable
private fun CoffeeFiltersPreview(dark: Boolean, compact: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val density = LocalDensity.current
    val state = CoffeeListState(filterDraft = CoffeeFilters(), filtersLoading = false,
        filterGroups = listOf(CoffeeFilterGroup("preview", "Варианты фильтра",
            List(8) { CoffeeFilterOption("option-$it", "Вариант ${it + 1}") })))
    CompositionLocalProvider(LocalDensity provides Density(density.density, if (compact) 2f else 1f)) {
        Surface(Modifier.width(if (compact) 320.dp else 390.dp).height(if (compact) 300.dp else 500.dp)) {
            CoffeeFiltersContent(state, {})
        }
    }
}

@Preview @Composable private fun CoffeeFiltersLightPreview() = CoffeeFiltersPreview(false)
@Preview @Composable private fun CoffeeFiltersDarkPreview() = CoffeeFiltersPreview(true)
@Preview @Composable private fun CoffeeFiltersCompactLargeLightPreview() = CoffeeFiltersPreview(false, true)
@Preview @Composable private fun CoffeeFiltersCompactLargeDarkPreview() = CoffeeFiltersPreview(true, true)
