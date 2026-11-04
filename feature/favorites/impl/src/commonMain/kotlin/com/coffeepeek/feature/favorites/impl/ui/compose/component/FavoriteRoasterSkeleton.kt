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
internal fun FavoriteRoasterSkeleton(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CoffeePeekShimmer(Modifier.size(64.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoffeePeekShimmer(Modifier.fillMaxWidth(0.8f).height(22.dp))
                    CoffeePeekShimmer(Modifier.fillMaxWidth().height(16.dp))
                }
            }
            CoffeePeekShimmer(Modifier.fillMaxWidth(0.8f).height(18.dp))
        }
    }
}

@Preview @Composable private fun FavoriteRoasterSkeletonLightPreview() = CoffeePeekTheme(darkTheme = false) { FavoriteRoasterSkeleton() }
@Preview @Composable private fun FavoriteRoasterSkeletonDarkPreview() = CoffeePeekTheme(darkTheme = true) { FavoriteRoasterSkeleton() }
