package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.ui.component.CheckmarkSection
import com.coffeepeek.admin.ui.component.GroupSection
import com.coffeepeek.admin.ui.component.PriceBeanSlider
import com.coffeepeek.admin.ui.component.RoasterAvatar
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import com.coffeepeek.admin.ui.component.priceLevelHint
import com.coffeepeek.admin.ui.component.shopTagIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CatalogItem
import coffeepeek.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val RowInset = 16.dp

/** Swipe down / back discards the draft; «Готово» applies it. */
@Composable
fun ShopFiltersScreen(state: FeedUiState, onDismiss: () -> Unit, onApply: (FeedFiltersUi) -> Unit) {
    var draft by remember(state.filters) { mutableStateOf(state.filters) }
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = onDismiss,
        dismissDescription = stringResource(Res.string.shop_filters_close),
        modifier = Modifier.dismissKeyboardOnBackgroundGestures(),
        maxSheetHeightFraction = 0.8f,
        expandToMaxHeight = true,
    ) {
        val column = this
        AnalyticsOverlay {
            with(column) {
                ShopFiltersContent(state, draft, { draft = it }) {
                    onApply(draft)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ShopFiltersContent(
    state: FeedUiState, draft: FeedFiltersUi, onDraft: (FeedFiltersUi) -> Unit, onDone: () -> Unit,
) {
    SheetNavBar(draft.activeFilterCount > 0, { onDraft(draft.clearSelections()) }, onDone)
    Column(
        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            .padding(horizontal = RowInset).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ShopPriceFilter(draft.priceRange) { onDraft(draft.copy(priceRange = it)) }
        ShopFilterSelections(state, draft, onDraft)
    }
}

@Composable
private fun ShopPriceFilter(priceRange: Int?, onSelect: (Int?) -> Unit) {
    GroupSection(
        title = stringResource(Res.string.shop_filters_price),
        trailing = priceLevelHint(priceRange) ?: stringResource(Res.string.shop_filters_any_price),
    ) {
        PriceBeanSlider(priceRange, onSelect, showTitle = false, showHint = false,
            modifier = Modifier.padding(horizontal = RowInset, vertical = 8.dp))
    }
}

@Composable
private fun ShopFilterSelections(state: FeedUiState, draft: FeedFiltersUi, onDraft: (FeedFiltersUi) -> Unit) {
    CheckmarkSection(
        title = stringResource(Res.string.shop_filters_roasters), items = state.roasters,
        selectedIds = draft.roasterIds,
        onToggle = { onDraft(draft.copy(roasterIds = draft.roasterIds.toggle(it))) },
        leading = { RoasterAvatar(it) },
    )
    CheckmarkSection(stringResource(Res.string.shop_filters_beans), state.beans, draft.beanIds,
        onToggle = { onDraft(draft.copy(beanIds = draft.beanIds.toggle(it))) })
    CheckmarkSection(stringResource(Res.string.shop_filters_equipment), state.equipment, draft.equipmentIds,
        onToggle = { onDraft(draft.copy(equipmentIds = draft.equipmentIds.toggle(it))) })
    ShopBrewFilter(state.brewMethods, draft.brewMethodIds) {
        onDraft(draft.copy(brewMethodIds = draft.brewMethodIds.toggle(it)))
    }
    ShopAmenityFilter(state.shopTags, draft.tagIds) { onDraft(draft.copy(tagIds = draft.tagIds.toggle(it))) }
}

@Composable
private fun ShopBrewFilter(items: List<CatalogItem>, selected: Set<String>, onToggle: (String) -> Unit) {
    CheckmarkSection(
        title = stringResource(Res.string.shop_filters_brew), items = items,
        selectedIds = selected, onToggle = onToggle,
        leading = {
            Icon(painterResource(brewMethodIcon(it.name)), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        },
    )
}

@Composable
private fun ShopAmenityFilter(items: List<CatalogItem>, selected: Set<String>, onToggle: (String) -> Unit) {
    CheckmarkSection(
        title = stringResource(Res.string.shop_filters_amenities), items = ShopTagGroups.amenityTags(items),
        selectedIds = selected, onToggle = onToggle,
        leading = {
            Icon(shopTagIcon(it.slug), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        },
    )
}

@Composable
private fun SheetNavBar(resetEnabled: Boolean, onReset: () -> Unit, onDone: () -> Unit) {
    if (LocalDensity.current.fontScale > 1.2f) {
        Column(Modifier.fillMaxWidth().padding(horizontal = RowInset)) {
            SheetTitle(Modifier.align(Alignment.CenterHorizontally))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SheetResetButton(resetEnabled, onReset)
                SheetDoneButton(onDone)
            }
        }
    } else {
        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp)) {
            Box(Modifier.align(Alignment.CenterStart)) { SheetResetButton(resetEnabled, onReset) }
            SheetTitle(Modifier.align(Alignment.Center))
            Box(Modifier.align(Alignment.CenterEnd)) { SheetDoneButton(onDone) }
        }
    }
}

@Composable
private fun SheetTitle(modifier: Modifier) {
    Text(stringResource(Res.string.shop_filters_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground, modifier = modifier)
}

@Composable
private fun SheetResetButton(enabled: Boolean, onReset: () -> Unit) {
    CoffeePeekTextButton(onReset, enabled = enabled) {
        Text(stringResource(Res.string.shop_filters_reset), fontSize = 17.sp)
    }
}

@Composable
private fun SheetDoneButton(onDone: () -> Unit) {
    CoffeePeekTextButton(onDone) {
        Text(stringResource(Res.string.shop_filters_done), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (contains(id)) this - id else this + id

@Composable
private fun ShopFiltersPreview(dark: Boolean, compact: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val choices = List(8) { CatalogItem("preview-$it", "Вариант фильтра ${it + 1}") }
    val state = FeedUiState(beans = choices, equipment = choices)
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, if (compact) 2f else 1f)) {
        Surface(Modifier.width(if (compact) 320.dp else 390.dp).height(if (compact) 300.dp else 500.dp)) {
            Column { ShopFiltersContent(state, FeedFiltersUi(), {}, {}) }
        }
    }
}

@Preview @Composable private fun ShopFiltersLightPreview() = ShopFiltersPreview(false)
@Preview @Composable private fun ShopFiltersDarkPreview() = ShopFiltersPreview(true)
@Preview @Composable private fun ShopFiltersCompactLargeLightPreview() = ShopFiltersPreview(false, true)
@Preview @Composable private fun ShopFiltersCompactLargeDarkPreview() = ShopFiltersPreview(true, true)
