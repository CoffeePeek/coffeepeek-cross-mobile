package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.CoffeePeekDrinkLabel
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Static saved-drink summary, not a new drink-detail navigation action. */
@Composable
internal fun ShopCheckInDrinkBadge(name: String, drinkPhotoUrl: String?, modifier: Modifier = Modifier) {
    val imageContent = LocalShopCheckInImage.current
    CoffeePeekDrinkLabel(name, modifier, imageContent = { imageModifier ->
        Box(imageModifier, contentAlignment = Alignment.Center) {
            if (drinkPhotoUrl.isNullOrBlank()) DrinkPhotoPlaceholder()
            else if (imageContent != null) imageContent(drinkPhotoUrl, Modifier.fillMaxSize(), ContentScale.Fit)
            else KamelImage(resource = { asyncPainterResource(drinkPhotoUrl) }, contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                onLoading = { DrinkPhotoPlaceholder() }, onFailure = { DrinkPhotoPlaceholder() })
        }
    })
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
