package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.ui.compose.component.DiscoveryRoasterAvatar
import com.coffeepeek.feature.discovery.impl.ui.compose.component.DiscoveryBrewMethodIcon
import com.coffeepeek.feature.discovery.impl.ui.compose.component.DiscoveryTagIcon
import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_close
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_collapse
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_done
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_reset
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_selected
import com.coffeepeek.feature.discovery.impl.resources.discovery_filters_show_all
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_beans
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_brew_methods
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_equipment
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_roasters
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_tags
import com.coffeepeek.core.designsystem.component.CoffeePeekActionRow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekCheckmarkRow
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCatalogs
import com.coffeepeek.feature.discovery.domain.rules.amenityTags
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

/**
 * The sheet edits a draft: «Готово» applies it with one reload, closing it without that drops the draft. The kind of the place is a quick filter,
 * not a sheet section (as in the previous sheet), and so is every switch; the sheet has the price, the roasters, the beans, the equipment, the brew methods
 * and the amenity tags.
 */
@Composable
internal fun FilterSheet(state: SearchState, onAction: (SearchAction) -> Unit) {
    if (PlatformDiscoveryFilterSheet(state, onAction)) return
    val draft = remember { DiscoveryFilterDraft(state.filters) }
    val density = LocalDensity.current
    val width = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = { onAction(SearchAction.CloseFilters) },
        dismissDescription = stringResource(Res.string.discovery_filters_close),
        modifier = Modifier.widthIn(max = width),
        maxSheetHeightFraction = 0.8f,
        expandToMaxHeight = true,
    ) {
        // The modal window must use the caller's text scale.
        CompositionLocalProvider(LocalDensity provides density) {
            FilterSheetContent(state, draft, {
                onAction(SearchAction.ApplyFilters(draft.applyTo(state.filters)))
                onAction(SearchAction.CloseFilters)
            }, { onAction(SearchAction.RetryCatalogs) })
        }
    }
}

@Composable
private fun ColumnScope.FilterSheetContent(
    state: SearchState, draft: DiscoveryFilterDraft,
    onDone: () -> Unit, onRetry: () -> Unit,
) {
    val filters by draft.state.collectAsState()
    FilterSheetHeader(filters.activeCount > 0, draft::reset, onDone)
    Column(
        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6),
    ) {
        FilterCatalogStatus(state, onRetry)
        FilterSelections(state, draft)
    }
}

@Composable
private fun FilterSheetHeader(resetEnabled: Boolean, onReset: () -> Unit, onDone: () -> Unit) {
    if (LocalDensity.current.fontScale > 1.2f) {
        Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4)) {
            Text(stringResource(Res.string.discovery_filters), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally))
            FlowRow(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onReset, enabled = resetEnabled, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(stringResource(Res.string.discovery_filters_reset), fontSize = 17.sp)
                }
                TextButton(onDone, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(stringResource(Res.string.discovery_filters_done), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp)) {
        TextButton(onReset, enabled = resetEnabled, modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                stringResource(Res.string.discovery_filters_reset), fontSize = 17.sp,
            )
        }
        Text(
            stringResource(Res.string.discovery_filters), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.align(Alignment.Center),
        )
        TextButton(onDone, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(stringResource(Res.string.discovery_filters_done), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterSelections(state: SearchState, draft: DiscoveryFilterDraft) {
    val filters by draft.state.collectAsState()
    PriceFilter(filters.priceLevel, draft::selectPrice)
    Selection(Res.string.discovery_section_roasters, state.catalogs.roasters, filters.roasterIds,
        { draft.toggle(DiscoveryFilterGroup.Roasters, it) }) { entry -> DiscoveryRoasterAvatar(entry.name, entry.photoUrl) }
    Selection(Res.string.discovery_section_beans, state.catalogs.beans, filters.beanIds,
        { draft.toggle(DiscoveryFilterGroup.Beans, it) })
    Selection(Res.string.discovery_section_equipment, state.catalogs.equipment, filters.equipmentIds,
        { draft.toggle(DiscoveryFilterGroup.Equipment, it) })
    Selection(Res.string.discovery_section_brew_methods, state.catalogs.brewMethods, filters.brewMethodIds,
        { draft.toggle(DiscoveryFilterGroup.BrewMethods, it) }, label = { filterBrewMethodLabel(it) }) { entry ->
        DiscoveryBrewMethodIcon(entry.name, 22.dp)
    }
    Selection(Res.string.discovery_section_tags, amenityTags(state.catalogs.tags), filters.tagIds,
        { draft.toggle(DiscoveryFilterGroup.Tags, it) }, label = { filterTagLabel(it) }) { entry ->
        DiscoveryTagIcon(entry.id)
    }
}

/** As in prod: the first five rows plus every selected one, then «Показать все (N)» / «Свернуть», and «Выбрано: N» beside the title. */
@Composable
private fun Selection(
    title: StringResource, entries: List<CatalogEntry>, selected: Set<String>, onToggle: (String) -> Unit,
    label: @Composable (CatalogEntry) -> String = { it.name }, leading: (@Composable (CatalogEntry) -> Unit)? = null,
) {
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
            CoffeePeekCheckmarkRow(label(entry), entry.id in selected, { onToggle(entry.id) }, leading = leading?.let { { it(entry) } })
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

@Preview
@Composable
private fun FilterSheetPreview() = CoffeePeekTheme {
    FilterSheet(SearchState()) {}
}

@Preview
@Composable
private fun FilterSheetHeaderLargePreview() = CoffeePeekTheme {
    CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(LocalDensity.current.density, 2f)) {
        Box(Modifier.width(320.dp)) { FilterSheetHeader(true, {}, {}) }
    }
}

@Preview
@Composable
private fun FilterSheetCompactLargePreview() = CoffeePeekTheme {
    val choices = List(8) { CatalogEntry("preview-$it", "Вариант фильтра ${it + 1}") }
    val state = SearchState(catalogs = DiscoveryCatalogs(beans = choices, equipment = choices),
        catalogLoadState = com.coffeepeek.feature.discovery.impl.search.CatalogLoadState.Ready)
    CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(LocalDensity.current.density, 2f)) {
        Surface(Modifier.width(320.dp).height(300.dp)) {
            Column { FilterSheetContent(state, remember { DiscoveryFilterDraft(state.filters) }, {}, {}) }
        }
    }
}

@Preview
@Composable
private fun FilterSheetBelarusianPreview() = CoffeePeekTheme {
    val catalogs = DiscoveryCatalogs(
        brewMethods = listOf(CatalogEntry("chemex", "Chemex"), CatalogEntry("batch-brew", "Batch Brew")),
        tags = listOf(CatalogEntry("roastery", "Roastery"), CatalogEntry("beans-for-sale", "Зерно на продажу"),
            CatalogEntry("decaf", "Decaf"), CatalogEntry("to-go", "To Go"),
            CatalogEntry("plan-based-milk", "Растительное молоко"), CatalogEntry("laptop-friendly", "Laptop Friendly")),
    )
    val state = SearchState(catalogs = catalogs,
        catalogLoadState = com.coffeepeek.feature.discovery.impl.search.CatalogLoadState.Ready)
    Surface(Modifier.width(360.dp).height(720.dp)) {
        Column { FilterSheetContent(state, remember { DiscoveryFilterDraft(state.filters) }, {}, {}) }
    }
}
