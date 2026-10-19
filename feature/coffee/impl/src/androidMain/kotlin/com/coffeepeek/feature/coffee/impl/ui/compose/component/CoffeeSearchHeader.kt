package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.impl.R

@Composable
internal fun CoffeeSearchHeader(query: String, filterCount: Int, onQueryChange: (String) -> Unit, onFilters: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekSearchField(query, onQueryChange, stringResource(R.string.coffee_search),
            stringResource(R.string.coffee_clear_search), modifier = Modifier.weight(1f))
        BadgedBox(badge = {
            if (filterCount > 0) Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center) {
                Text(filterCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }) {
            OutlinedIconButton(onFilters, Modifier.size(CoffeePeekDimens.buttonHeight), shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Icon(CoffeePeekIcons.Filter, if (filterCount > 0) stringResource(R.string.coffee_filters_count, filterCount)
                    else stringResource(R.string.coffee_filters), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@PreviewLightDark @Composable private fun CoffeeSearchHeaderPreview() = CoffeePeekTheme {
    Surface { CoffeeSearchHeader("", 2, {}, {}) }
}
