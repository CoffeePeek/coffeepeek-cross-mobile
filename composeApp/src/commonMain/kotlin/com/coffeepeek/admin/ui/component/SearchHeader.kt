package com.coffeepeek.admin.ui.component

import com.coffeepeek.admin.analytics.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedIconButton as OutlinedIconButton
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.search_roasters_placeholder
import coffeepeek.composeapp.generated.resources.search_shops_and_roasters_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    roastersSelected: Boolean = false,
    onSelectRoasters: (Boolean) -> Unit = {},
    filterCount: Int = 0,
    analyticsCatalog: String = "shops",
    analyticsScreen: String = "discovery",
    onFilters: (() -> Unit)? = null,
    showCategories: Boolean = true,
    onSearchFocus: () -> Unit = {},
    onCancelSearch: (() -> Unit)? = null,
    placeholder: String = stringResource(
        if (roastersSelected) Res.string.search_roasters_placeholder
        else Res.string.search_shops_and_roasters_placeholder,
    ),
    modifier: Modifier = Modifier,
) {
    SearchHeaderControls(query, onQueryChange, filterCount, analyticsCatalog, analyticsScreen, onFilters,
        onSearchFocus, onCancelSearch, placeholder, modifier)
    if (showCategories) {
        Spacer(Modifier.height(CpDimens.spacing2))
        SearchCategories(roastersSelected, onSelectRoasters)
    }
}

@Composable
private fun SearchHeaderControls(
    query: String, onQueryChange: (String) -> Unit, filterCount: Int, analyticsCatalog: String,
    analyticsScreen: String, onFilters: (() -> Unit)?, onSearchFocus: () -> Unit,
    onCancelSearch: (() -> Unit)?, placeholder: String, modifier: Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = onCancelSearch != null,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CpCircularBackButton(onClick = { onCancelSearch?.invoke() }, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(CpDimens.spacing2))
            }
        }
        CpSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f).analyticsExposure("search", analyticsCatalog, analyticsScreen),
            onFocusChanged = { if (it) onSearchFocus() },
            onCancelSearch = onCancelSearch,
            fieldHeight = CpDimens.buttonHeight,
        )
        if (onFilters != null) {
            Spacer(Modifier.width(CpDimens.spacing2))
            SearchFilterButton(filterCount, analyticsCatalog, analyticsScreen, onFilters)
        }
    }
}

@Composable
private fun SearchFilterButton(filterCount: Int, analyticsCatalog: String, analyticsScreen: String, onClick: () -> Unit) {
    BadgedBox(badge = {
        if (filterCount > 0) Box(
            Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(filterCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }) {
        OutlinedIconButton(
            onClick = onClick, modifier = Modifier.size(CpDimens.buttonHeight).analyticsExposure("filters", analyticsCatalog, analyticsScreen), shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Icon(CpIcons.Filter, "Фильтры", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SearchCategories(roastersSelected: Boolean, onSelectRoasters: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup().clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp),
    ) {
        listOf(false, true).forEach { roasters ->
            val selected = roastersSelected == roasters
            val shape = RoundedCornerShape(percent = 50)
            Row(
                modifier = Modifier.weight(1f).height(40.dp).clip(shape)
                    .background(if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelectRoasters(roasters) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    if (roasters) CpIcons.Factory else CpIcons.Coffee, null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(if (roasters) "Обжарщики" else "Кофейни", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}


@Composable
private fun SearchHeaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SearchHeader("", {}, showCategories = false)
            SearchHeader("Coffee", {}, showCategories = false, onCancelSearch = {}, onFilters = {})
        }
    }
}

@Preview @Composable private fun SearchHeaderLightPreview() = SearchHeaderPreview(false)
@Preview @Composable private fun SearchHeaderDarkPreview() = SearchHeaderPreview(true)
