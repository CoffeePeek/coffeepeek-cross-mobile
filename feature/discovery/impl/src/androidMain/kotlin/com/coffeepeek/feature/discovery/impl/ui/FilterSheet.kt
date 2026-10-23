package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCheckmarkRow
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.rules.amenityTags
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

/**
 * The sheet edits a draft: «Готово» applies it with one reload, closing it without that drops the draft. The kind of the place is a quick filter,
 * not a sheet section (as in the legacy sheet), and so is every switch; the sheet has the price, the roasters, the beans, the equipment, the brew methods
 * and the amenity tags.
 */
@Composable
internal fun FilterSheet(state: SearchState, host: DiscoveryHost, onAction: (SearchAction) -> Unit) {
    var draft by remember { mutableStateOf(state.filters) }
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = { onAction(SearchAction.CloseFilters) },
        dismissDescription = stringResource(R.string.discovery_filters_close),
        maxSheetHeightFraction = 0.92f,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing1), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ draft = draft.clearSelections() }, enabled = draft.activeCount > 0) { Text(stringResource(R.string.discovery_filters_reset)) }
            Text(stringResource(R.string.discovery_filters), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton({ onAction(SearchAction.ApplyFilters(draft)); onAction(SearchAction.CloseFilters) }) { Text(stringResource(R.string.discovery_filters_done)) }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6),
        ) {
            CoffeePeekGroupSection(
                title = stringResource(R.string.discovery_section_price),
                trailing = host.priceHint(draft.priceLevel?.level) ?: stringResource(R.string.discovery_price_any),
            ) {
                host.priceSelector(draft.priceLevel?.level, { draft = draft.copy(priceLevel = PriceLevel.ofLevel(it)) }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Selection(R.string.discovery_section_roasters, state.catalogs.roasters, draft.roasterIds, { draft = draft.copy(roasterIds = draft.roasterIds.toggle(it)) }) { entry ->
                host.roasterAvatar(entry.name, entry.photoUrl)
            }
            Selection(R.string.discovery_section_beans, state.catalogs.beans, draft.beanIds, { draft = draft.copy(beanIds = draft.beanIds.toggle(it)) })
            Selection(R.string.discovery_section_equipment, state.catalogs.equipment, draft.equipmentIds, { draft = draft.copy(equipmentIds = draft.equipmentIds.toggle(it)) })
            Selection(R.string.discovery_section_brew_methods, state.catalogs.brewMethods, draft.brewMethodIds, { draft = draft.copy(brewMethodIds = draft.brewMethodIds.toggle(it)) }) { entry ->
                host.brewMethodIcon(entry.name, 22.dp)
            }
            Selection(R.string.discovery_section_tags, amenityTags(state.catalogs.tags), draft.tagIds, { draft = draft.copy(tagIds = draft.tagIds.toggle(it)) }) { entry ->
                host.tagIcon(entry.id)
            }
        }
    }
}

@Composable
private fun Selection(title: Int, entries: List<CatalogEntry>, selected: Set<String>, onToggle: (String) -> Unit, leading: (@Composable (CatalogEntry) -> Unit)? = null) {
    if (entries.isEmpty()) return
    CoffeePeekGroupSection(title = stringResource(title)) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) CoffeePeekRowSeparator()
            CoffeePeekCheckmarkRow(entry.name, entry.id in selected, { onToggle(entry.id) }, leading = leading?.let { { it(entry) } })
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
