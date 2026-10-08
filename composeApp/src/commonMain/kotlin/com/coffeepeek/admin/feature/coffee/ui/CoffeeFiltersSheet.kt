package com.coffeepeek.admin.feature.coffee.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
    var draft by remember(state.filters) { mutableStateOf(state.filters) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing1), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { draft = CoffeeFilters() }, enabled = draft.activeCount > 0) { Text("Сбросить") }
            Text("Фильтры", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = { onApply(draft) }) { Text("Готово") }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = CpDimens.spacing4).padding(bottom = CpDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
        ) {
            GroupSection { SwitchRow("Только в наличии", draft.availableOnly) { draft = draft.copy(availableOnly = it) } }
            when {
                state.filtersLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                state.filtersError -> Column {
                    Text("Не удалось загрузить фильтры")
                    TextButton(onClick = onRetry) { Text("Попробовать снова") }
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
