package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun FavoritesLoadingSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.favorites_loading)
    LazyColumn(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).clearAndSetSemantics {
        contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false) {
        item { FavoriteShopSkeleton() }
        item { Text(stringResource(Res.string.favorites_roasters), style = MaterialTheme.typography.headlineSmall) }
        item { FavoriteRoasterSkeleton() }
    }
}

@Preview @Composable private fun FavoritesLoadingSkeletonLightPreview() = CoffeePeekTheme(darkTheme = false) { FavoritesLoadingSkeleton() }
@Preview @Composable private fun FavoritesLoadingSkeletonDarkPreview() = CoffeePeekTheme(darkTheme = true) { FavoritesLoadingSkeleton() }
