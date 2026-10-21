package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.domain.model.RoasterAddress
import com.coffeepeek.feature.roaster.domain.model.RoasterShop
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_open_shop
import com.coffeepeek.feature.roaster.impl.resources.roaster_shop_photo
import com.coffeepeek.feature.roaster.impl.resources.roaster_shop_photo_missing
import com.coffeepeek.feature.roaster.api.RoasterImage

@Composable
internal fun RoasterShopCard(shop: RoasterShop, onClick: () -> Unit, image: RoasterImage, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing1)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))) {
                val photo = shop.photoUrl?.takeIf(String::isNotBlank)
                if (photo != null) image(photo, stringResource(Res.string.roaster_shop_photo, shop.name), Modifier.fillMaxSize(), ContentScale.Crop, 9.sp)
                else CoffeePeekImagePlaceholder(labelSize = 9.sp, contentDescription = stringResource(Res.string.roaster_shop_photo_missing, shop.name))
            }
            Text(shop.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = CoffeePeekDimens.spacing3))
            Icon(CoffeePeekIcons.ChevronRight, contentDescription = stringResource(Res.string.roaster_open_shop, shop.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview @Composable private fun RoasterShopCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterShopCard(RoasterShop("cafe", "Sandbox Cafe", null, RoasterAddress("cafe", "/coffee-shops/cafe", 1, false)), {}, { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterShopCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterShopCard(RoasterShop("cafe", "Sandbox Cafe", null, RoasterAddress("cafe", "/coffee-shops/cafe", 1, false)), {}, { _, _, _, _, _ -> })
}
