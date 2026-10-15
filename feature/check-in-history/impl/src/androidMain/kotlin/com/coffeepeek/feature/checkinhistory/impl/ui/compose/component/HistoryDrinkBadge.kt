package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HistoryDrinkBadge(name: String, drinkPhotoUrl: String?, modifier: Modifier = Modifier,
    imageContent: HistoryImageContent? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        if (drinkPhotoUrl.isNullOrBlank()) Icon(CoffeePeekIcons.Coffee, null, Modifier.size(16.dp))
        else HistoryPhoto(drinkPhotoUrl, Modifier.size(44.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)),
            ContentScale.Fit, imageContent)
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@PreviewLightDark @Composable private fun HistoryDrinkBadgePreview() = CoffeePeekTheme {
    HistoryDrinkBadge("Капучино", null)
}
