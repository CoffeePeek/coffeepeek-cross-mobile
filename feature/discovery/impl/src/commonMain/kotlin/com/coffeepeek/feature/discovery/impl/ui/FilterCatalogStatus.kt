package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_catalog_empty
import com.coffeepeek.feature.discovery.impl.resources.discovery_catalog_failed
import com.coffeepeek.feature.discovery.impl.resources.discovery_catalog_loading
import com.coffeepeek.feature.discovery.impl.resources.discovery_retry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRefreshError
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.search.CatalogLoadState
import com.coffeepeek.feature.discovery.impl.search.SearchState

@Composable
internal fun FilterCatalogStatus(state: SearchState, onRetry: () -> Unit) {
    when (state.catalogLoadState) {
        CatalogLoadState.Loading -> Row(
            Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoffeePeekLoader(size = 24.dp, contentDescription = stringResource(Res.string.discovery_catalog_loading))
            Text(stringResource(Res.string.discovery_catalog_loading))
        }
        CatalogLoadState.Failed -> CoffeePeekRefreshError(
            onRetry, message = stringResource(Res.string.discovery_catalog_failed),
            retryLabel = stringResource(Res.string.discovery_retry),
        )
        CatalogLoadState.Ready -> if (state.hasNoCatalogChoices) Text(
            stringResource(Res.string.discovery_catalog_empty), color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun FilterCatalogStatusPreview() = CoffeePeekTheme {
    Surface {
        Column {
            CatalogLoadState.entries.forEach { FilterCatalogStatus(SearchState(catalogLoadState = it)) {} }
        }
    }
}
