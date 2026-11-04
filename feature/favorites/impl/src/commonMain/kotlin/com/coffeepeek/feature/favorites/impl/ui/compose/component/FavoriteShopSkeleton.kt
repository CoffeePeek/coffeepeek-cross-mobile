package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekShimmer
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FavoriteShopSkeleton(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            CoffeePeekShimmer(Modifier.fillMaxWidth().aspectRatio(2f), shape = RoundedCornerShape(0.dp))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CoffeePeekShimmer(Modifier.fillMaxWidth(0.7f).height(24.dp))
                CoffeePeekShimmer(Modifier.fillMaxWidth(0.9f).height(16.dp))
                CoffeePeekShimmer(Modifier.fillMaxWidth(0.45f).height(16.dp))
            }
        }
    }
}

@Preview @Composable private fun FavoriteShopSkeletonLightPreview() = CoffeePeekTheme(darkTheme = false) { FavoriteShopSkeleton() }
@Preview @Composable private fun FavoriteShopSkeletonDarkPreview() = CoffeePeekTheme(darkTheme = true) { FavoriteShopSkeleton() }
