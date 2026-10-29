package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_close
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_collapse
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_done
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_reset
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_selected
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_show_all
import com.coffeepeek.feature.discovery.impl.resources.discovery_price_any
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_beans
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_brew_methods
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_equipment
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_price
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_roasters
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_tags
import com.coffeepeek.core.designsystem.component.CoffeePeekActionRow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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
        dismissDescription = stringResource(Res.string.discovery_filters_close),
    ) {
        // The navigation bar of prod's sheet: 48 dp, 17 sp, the title centred.
        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp)) {
            val resetEnabled = draft.activeCount > 0
            TextButton({ draft = draft.clearSelections() }, enabled = resetEnabled, modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    stringResource(Res.string.discovery_filters_reset), fontSize = 17.sp,
                    color = if (resetEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            Text(
                stringResource(Res.string.discovery_filters), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.align(Alignment.Center),
            )
            TextButton({ onAction(SearchAction.ApplyFilters(draft)); onAction(SearchAction.CloseFilters) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text(stringResource(Res.string.discovery_filters_done), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6),
        ) {
            CoffeePeekGroupSection(
                title = stringResource(Res.string.discovery_section_price),
                trailing = host.priceHint(draft.priceLevel?.level) ?: stringResource(Res.string.discovery_price_any),
            ) {
                host.priceSelector(draft.priceLevel?.level, { draft = draft.copy(priceLevel = PriceLevel.ofLevel(it)) }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Selection(Res.string.discovery_section_roasters, state.catalogs.roasters, draft.roasterIds, { draft = draft.copy(roasterIds = draft.roasterIds.toggle(it)) }) { entry ->
                host.roasterAvatar(entry.name, entry.photoUrl)
            }
            Selection(Res.string.discovery_section_beans, state.catalogs.beans, draft.beanIds, { draft = draft.copy(beanIds = draft.beanIds.toggle(it)) })
            Selection(Res.string.discovery_section_equipment, state.catalogs.equipment, draft.equipmentIds, { draft = draft.copy(equipmentIds = draft.equipmentIds.toggle(it)) })
            Selection(Res.string.discovery_section_brew_methods, state.catalogs.brewMethods, draft.brewMethodIds, { draft = draft.copy(brewMethodIds = draft.brewMethodIds.toggle(it)) }) { entry ->
                host.brewMethodIcon(entry.name, 22.dp)
            }
            Selection(Res.string.discovery_section_tags, amenityTags(state.catalogs.tags), draft.tagIds, { draft = draft.copy(tagIds = draft.tagIds.toggle(it)) }) { entry ->
                host.tagIcon(entry.id)
            }
        }
    }
}

/** As in prod: the first five rows plus every selected one, then «Показать все (N)» / «Свернуть», and «Выбрано: N» beside the title. */
@Composable
private fun Selection(title: StringResource, entries: List<CatalogEntry>, selected: Set<String>, onToggle: (String) -> Unit, leading: (@Composable (CatalogEntry) -> Unit)? = null) {
    if (entries.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val collapsible = entries.size > COLLAPSED_ROWS
    val visible = if (expanded || !collapsible) entries else (entries.take(COLLAPSED_ROWS) + entries.filter { it.id in selected }).distinctBy { it.id }
    val selectedCount = entries.count { it.id in selected }
    CoffeePeekGroupSection(
        title = stringResource(title),
        trailing = selectedCount.takeIf { it > 0 }?.let { stringResource(Res.string.discovery_filters_selected, it) },
    ) {
        visible.forEachIndexed { index, entry ->
            if (index > 0) CoffeePeekRowSeparator()
            CoffeePeekCheckmarkRow(entry.name, entry.id in selected, { onToggle(entry.id) }, leading = leading?.let { { it(entry) } })
        }
        if (collapsible) {
            CoffeePeekRowSeparator()
            CoffeePeekActionRow(
                label = if (expanded) stringResource(Res.string.discovery_filters_collapse) else stringResource(Res.string.discovery_filters_show_all, entries.size),
                onClick = { expanded = !expanded },
            )
        }
    }
}

private const val COLLAPSED_ROWS = 5

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
