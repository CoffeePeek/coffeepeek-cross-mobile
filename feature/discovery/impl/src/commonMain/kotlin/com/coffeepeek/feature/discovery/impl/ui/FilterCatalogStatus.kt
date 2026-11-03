package com.coffeepeek.feature.discovery.impl.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRefreshError
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.CatalogLoadState
import com.coffeepeek.feature.discovery.impl.search.SearchState

@Composable
internal fun FilterCatalogStatus(state: SearchState, onRetry: () -> Unit) {
    when (state.catalogLoadState) {
        CatalogLoadState.Loading -> Row(
            Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoffeePeekLoader(size = 24.dp, contentDescription = stringResource(R.string.discovery_catalog_loading))
            Text(stringResource(R.string.discovery_catalog_loading))
        }
        CatalogLoadState.Failed -> CoffeePeekRefreshError(
            onRetry, message = stringResource(R.string.discovery_catalog_failed),
            retryLabel = stringResource(R.string.discovery_retry),
        )
        CatalogLoadState.Ready -> if (state.hasNoCatalogChoices) Text(
            stringResource(R.string.discovery_catalog_empty), color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun FilterCatalogStatusPreview() = CoffeePeekTheme {
    Surface {
        Column {
            CatalogLoadState.entries.forEach { FilterCatalogStatus(SearchState(catalogLoadState = it)) {} }
        }
    }
}
