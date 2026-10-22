package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.rules.amenityTags
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

private val RatingSteps = listOf(3.5, 4.0, 4.5)

/** The sheet edits a draft; «Готово» applies it with one reload, closing it without that drops the draft. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(state: SearchState, onAction: (SearchAction) -> Unit) {
    var draft by remember { mutableStateOf(state.filters) }
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = { onAction(SearchAction.CloseFilters) },
        dismissDescription = stringResource(R.string.discovery_filters_close),
        maxSheetHeightFraction = 0.92f,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.discovery_filters), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { draft = draft.clearSelections() }) { Text(stringResource(R.string.discovery_filters_reset)) }
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
            Section(R.string.discovery_section_kind) {
                ShopKind.entries.forEach { kind ->
                    CoffeePeekFilterChip(stringResource(kind.label()), draft.kind == kind, { draft = draft.copy(kind = kind.takeUnless { draft.kind == it }) })
                }
            }
            Section(R.string.discovery_section_price) {
                PriceLevel.entries.forEach { level ->
                    CoffeePeekFilterChip("$".repeat(level.level), draft.priceLevel == level, { draft = draft.copy(priceLevel = level.takeUnless { draft.priceLevel == it }) })
                }
            }
            Section(R.string.discovery_section_rating) {
                RatingSteps.forEach { rating ->
                    CoffeePeekFilterChip(stringResource(R.string.discovery_rating_from, rating.toString()), draft.minRating == rating, { draft = draft.copy(minRating = rating.takeUnless { draft.minRating == it }) })
                }
            }
            CatalogSection(R.string.discovery_section_roasters, state.catalogs.roasters, draft.roasterIds) { draft = draft.copy(roasterIds = draft.roasterIds.toggle(it)) }
            CatalogSection(R.string.discovery_section_beans, state.catalogs.beans, draft.beanIds) { draft = draft.copy(beanIds = draft.beanIds.toggle(it)) }
            CatalogSection(R.string.discovery_section_equipment, state.catalogs.equipment, draft.equipmentIds) { draft = draft.copy(equipmentIds = draft.equipmentIds.toggle(it)) }
            CatalogSection(R.string.discovery_section_brew_methods, state.catalogs.brewMethods, draft.brewMethodIds) { draft = draft.copy(brewMethodIds = draft.brewMethodIds.toggle(it)) }
            CatalogSection(R.string.discovery_section_tags, amenityTags(state.catalogs.tags), draft.tagIds) { draft = draft.copy(tagIds = draft.tagIds.toggle(it)) }
        }
        CoffeePeekButton(
            stringResource(R.string.discovery_filters_done),
            onClick = { onAction(SearchAction.ApplyFilters(draft)); onAction(SearchAction.CloseFilters) },
            modifier = Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) { content() }
    }
}

@Composable
private fun CatalogSection(title: Int, entries: List<CatalogEntry>, selected: Set<String>, onToggle: (String) -> Unit) {
    if (entries.isEmpty()) return
    Section(title) { entries.forEach { CoffeePeekFilterChip(it.name, it.id in selected, { onToggle(it.id) }) } }
}

private fun ShopKind.label() = when (this) {
    ShopKind.Specialty -> R.string.discovery_kind_specialty
    ShopKind.CoffeeBar -> R.string.discovery_kind_coffee_bar
    ShopKind.Cafe -> R.string.discovery_kind_cafe
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
