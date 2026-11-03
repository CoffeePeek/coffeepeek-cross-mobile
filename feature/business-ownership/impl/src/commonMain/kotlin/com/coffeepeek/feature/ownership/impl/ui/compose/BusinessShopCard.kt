package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekShopCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.OwnershipImage
import com.coffeepeek.feature.ownership.domain.model.BusinessKind
import com.coffeepeek.feature.ownership.domain.model.BusinessProfile
import com.coffeepeek.feature.ownership.domain.model.OwnedBusiness
import com.coffeepeek.feature.ownership.impl.resources.Res
import com.coffeepeek.feature.ownership.impl.resources.ownership_shop_kind
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BusinessShopCard(business: OwnedBusiness, profile: BusinessProfile?, image: OwnershipImage,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    val photo = profile?.photos?.firstOrNull()
    CoffeePeekShopCard(title = business.name, kindLabel = stringResource(Res.string.ownership_shop_kind),
        modifier = modifier, locationSummary = profile?.address?.takeIf(String::isNotBlank), onClick = onClick,
        cover = {
            if (photo?.url != null || photo?.bytes != null) image(photo.url, photo.bytes, Modifier.fillMaxSize(), ContentScale.Crop)
            else CoffeePeekImagePlaceholder(Modifier.fillMaxSize())
        })
}

@Composable private fun BusinessShopCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    BusinessShopCard(OwnedBusiness("preview", BusinessKind.CoffeeShop, "Why Knot?"),
        BusinessProfile("preview", BusinessKind.CoffeeShop, 1, "Why Knot?", address = "ул. Володарского, 21"),
        { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) }, {})
}

@Preview @Composable private fun BusinessShopCardLightPreview() = BusinessShopCardPreview(false)
@Preview @Composable private fun BusinessShopCardDarkPreview() = BusinessShopCardPreview(true)
