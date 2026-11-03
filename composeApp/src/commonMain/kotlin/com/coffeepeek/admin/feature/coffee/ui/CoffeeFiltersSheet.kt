package com.coffeepeek.admin.feature.coffee.ui

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CheckmarkRow
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.GroupSection
import com.coffeepeek.admin.ui.component.RowSeparator
import com.coffeepeek.admin.ui.component.SwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoffeeFiltersSheet(
    state: CoffeeListUiState,
    onDismiss: () -> Unit,
    onApply: (CoffeeFilters) -> Unit,
    onRetry: () -> Unit,
) {
    val localizedShopFiltersReset = stringResource(Res.string.shop_filters_reset)
    val localizedShopFiltersTitle = stringResource(Res.string.shop_filters_title)
    val localizedStepDone = stringResource(Res.string.step_done)
    val localizedLegacyCoffeeFiltersSheetCoffeeAvailableOnly = stringResource(Res.string.legacy_coffee_filters_sheet_coffee_available_only)
    val localizedLegacyCoffeeFiltersSheetCoffeeFiltersFailed = stringResource(Res.string.legacy_coffee_filters_sheet_coffee_filters_failed)
    val localizedRetry = stringResource(Res.string.retry)

    var draft by remember(state.filters) { mutableStateOf(state.filters) }
    SwipeDismissModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing1), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { draft = CoffeeFilters() }, enabled = draft.activeCount > 0) { Text(localizedShopFiltersReset) }
            Text(localizedShopFiltersTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = { onApply(draft) }) { Text(localizedStepDone) }
        }
        PlatformScrollableColumn(
            Modifier.weight(1f, fill = false).navigationBarsPadding()
                .padding(horizontal = CpDimens.spacing4).padding(bottom = CpDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
        ) {
            GroupSection { SwitchRow(localizedLegacyCoffeeFiltersSheetCoffeeAvailableOnly, draft.availableOnly) { draft = draft.copy(availableOnly = it) } }
            when {
                state.filtersLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                state.filtersError -> Column {
                    Text(localizedLegacyCoffeeFiltersSheetCoffeeFiltersFailed)
                    TextButton(onClick = onRetry) { Text(localizedRetry) }
                }
                else -> state.filterGroups.forEach { group ->
                    GroupSection(title = group.name) {
                        group.options.forEachIndexed { index, option ->
                            if (index > 0) RowSeparator()
                            CheckmarkRow(option.name, option.code in draft.values[group.code].orEmpty(), onToggle = {
                                draft = draft.toggle(group.code, option.code)
                            })
                        }
                    }
                }
            }
        }
    }
}
