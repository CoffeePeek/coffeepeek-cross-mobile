package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.coffee.impl.resources.Res
import com.coffeepeek.feature.coffee.impl.resources.coffee_clear_search
import com.coffeepeek.feature.coffee.impl.resources.coffee_filters
import com.coffeepeek.feature.coffee.impl.resources.coffee_filters_count
import com.coffeepeek.feature.coffee.impl.resources.coffee_search
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.impl.resources.*
import com.coffeepeek.feature.coffee.api.CoffeeScreenAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeExposure

@Composable
internal fun CoffeeSearchHeader(query: String, filterCount: Int, onQueryChange: (String) -> Unit, onFilters: () -> Unit,
    screen: CoffeeScreenAnalytics = CoffeeScreenAnalytics.None) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekSearchField(query, onQueryChange, stringResource(Res.string.coffee_search),
            stringResource(Res.string.coffee_clear_search), modifier = screen.exposure(Modifier.weight(1f), CoffeeExposure.Search))
        BadgedBox(badge = {
            if (filterCount > 0) Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center) {
                Text(filterCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }) {
            OutlinedIconButton(onFilters, screen.exposure(Modifier.size(CoffeePeekDimens.buttonHeight), CoffeeExposure.Filters), shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Icon(CoffeePeekIcons.Filter, if (filterCount > 0) stringResource(Res.string.coffee_filters_count, filterCount)
                    else stringResource(Res.string.coffee_filters), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable private fun CoffeeSearchHeaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeeSearchHeader("", 2, {}, {}) }
}
@Preview @Composable private fun CoffeeSearchHeaderLightPreview() = CoffeeSearchHeaderPreview(false)
@Preview @Composable private fun CoffeeSearchHeaderDarkPreview() = CoffeeSearchHeaderPreview(true)
