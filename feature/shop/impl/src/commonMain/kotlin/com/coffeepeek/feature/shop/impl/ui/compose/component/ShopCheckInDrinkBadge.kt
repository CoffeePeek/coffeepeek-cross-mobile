package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Static saved-drink summary, not a new drink-detail navigation action. */
@Composable
internal fun ShopCheckInDrinkBadge(name: String, drinkPhotoUrl: String?, modifier: Modifier = Modifier) {
    val imageContent = LocalShopCheckInImage.current
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(CoffeePeekDimens.radiusLg))
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)).padding(CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)),
            contentAlignment = Alignment.Center) {
            if (drinkPhotoUrl.isNullOrBlank()) DrinkPhotoPlaceholder()
            else if (imageContent != null) imageContent(drinkPhotoUrl, Modifier.fillMaxSize(), ContentScale.Fit)
            else KamelImage(resource = { asyncPainterResource(drinkPhotoUrl) }, contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                onLoading = { DrinkPhotoPlaceholder() }, onFailure = { DrinkPhotoPlaceholder() })
        }
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(CoffeePeekIcons.ChevronRight, null, modifier = Modifier.size(18.dp).padding(end = CoffeePeekDimens.spacing1))
    }
}

@Composable
private fun DrinkPhotoPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(CoffeePeekIcons.Coffee, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun ShopCheckInDrinkLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInDrinkBadge("Фильтр", null)
}
@Preview @Composable private fun ShopCheckInDrinkDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInDrinkBadge("Фильтр", null)
}
