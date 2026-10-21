package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingDisplay
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun RatingBadge(rating: Double, reviewCount: Int) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusLg))
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(horizontal = CoffeePeekDimens.spacing2, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoffeePeekRating(rating, size = CoffeePeekRatingSize.Small, display = CoffeePeekRatingDisplay.Compact,
            activeColor = CoffeePeekColors.Primary, valueColor = Color.White, valueSpacing = CoffeePeekDimens.spacing1,
            valueStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
        if (reviewCount > 0) {
            Text("($reviewCount)", color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview @Composable private fun RatingBadgeLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RatingBadge(4.8, 12)
}
@Preview @Composable private fun RatingBadgeDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RatingBadge(4.8, 12)
}
