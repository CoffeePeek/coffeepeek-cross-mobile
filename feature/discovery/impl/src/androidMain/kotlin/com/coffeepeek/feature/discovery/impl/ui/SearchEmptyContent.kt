package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.SearchEmptyReason

@Composable
internal fun SearchEmptyContent(
    reason: SearchEmptyReason, allResultsEmpty: Boolean, hasQuery: Boolean,
    onEditQuery: () -> Unit, onResetFilters: () -> Unit,
    onAddShop: () -> Unit, onAddRoaster: (() -> Unit)?, illustration: Painter,
) {
    val title = when (reason) {
        SearchEmptyReason.Catalog -> R.string.discovery_empty_catalog_title
        SearchEmptyReason.Filters -> R.string.discovery_empty_filters_title
        SearchEmptyReason.Query -> if (allResultsEmpty) R.string.discovery_empty_query_title else R.string.discovery_empty_shops_title
    }
    val description = when (reason) {
        SearchEmptyReason.Catalog -> R.string.discovery_empty_catalog_body
        SearchEmptyReason.Filters -> R.string.discovery_empty_filters_body
        SearchEmptyReason.Query -> R.string.discovery_empty_query_body
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!allResultsEmpty) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (reason == SearchEmptyReason.Filters) {
                CoffeePeekButton(stringResource(R.string.discovery_reset_filters), onResetFilters)
                if (hasQuery) CoffeePeekTextButton(onEditQuery) { Text(stringResource(R.string.discovery_edit_query)) }
            } else if (hasQuery) CoffeePeekButton(stringResource(R.string.discovery_edit_query), onEditQuery)
        }
        SearchContributionActions(illustration, onAddShop, onAddRoaster)
    }
}

@PreviewLightDark
@Composable
private fun SearchEmptyContentPreview() = CoffeePeekTheme {
    Surface {
        SearchEmptyContent(SearchEmptyReason.Query, true, true, {}, {}, {}, {}, ColorPainter(Color.LightGray))
    }
}

@PreviewLightDark
@Composable
private fun FilteredGlobalEmptySearchPreview() = CoffeePeekTheme {
    Surface {
        SearchEmptyContent(SearchEmptyReason.Filters, true, true, {}, {}, {}, {}, ColorPainter(Color.LightGray))
    }
}

@PreviewLightDark
@Composable
private fun SearchFilterRecoveryPreview() = CoffeePeekTheme {
    Surface {
        SearchEmptyContent(SearchEmptyReason.Filters, false, false, {}, {}, {}, null, ColorPainter(Color.LightGray))
    }
}
