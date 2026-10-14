package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_rating_value
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInRatingCard(image: DrawableResource, label: String, rating: Int,
    enabled: Boolean, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    Row(Modifier.fillMaxWidth().clip(shape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
        .padding(horizontal = CoffeePeekDimens.spacing2, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Image(painterResource(image), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusSm)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                (1..5).forEach { star ->
                    val description = stringResource(Res.string.shop_review_rating_value, label, star)
                    Box(Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button,
                        onClickLabel = description) { onChange(star) }
                        .semantics { selected = star == rating }, contentAlignment = Alignment.Center) {
                        Icon(if (star <= rating) CoffeePeekIcons.StarFilled else CoffeePeekIcons.StarOutline,
                            description, Modifier.size(24.dp), tint = if (star <= rating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                    }
                }
            }
        }
    }
}

@Composable private fun ShopCheckInRatingCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShopCheckInRatingCard(Res.drawable.shop_checkin_rating_coffee, stringResource(Res.string.shop_review_coffee), 4, true, {})
}
@Preview @Composable private fun ShopCheckInRatingCardLightPreview() = ShopCheckInRatingCardPreview(false)
@Preview @Composable private fun ShopCheckInRatingCardDarkPreview() = ShopCheckInRatingCardPreview(true)
