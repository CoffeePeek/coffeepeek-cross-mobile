package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_catalog
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_logo
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_remove
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_shops
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Production catalogue geometry and theme colors, without importing catalogue/legacy UI. */
@Composable
internal fun FavoriteRoasterCard(
    roaster: FavoriteRoaster,
    removing: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(enabled = !roaster.publicSlug.isNullOrBlank(), role = Role.Button, onClick = onOpen),
        shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
                .background(if (roaster.photoUrl != null) Color.White else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                val photo = roaster.photoUrl?.takeIf(String::isNotBlank)
                if (photo != null) KamelImage(
                    resource = { asyncPainterResource(photo) },
                    contentDescription = stringResource(Res.string.favorites_roaster_logo, roaster.title),
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                    onLoading = { RoasterPlaceholder() }, onFailure = { RoasterPlaceholder() },
                ) else RoasterPlaceholder()
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(roaster.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    CoffeePeekLikeButton(true, stringResource(Res.string.favorites_roaster_remove, roaster.title), onRemove,
                        enabled = !removing && roaster.id.isNotBlank(), modifier = Modifier.size(48.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FavoriteRoasterStat(CoffeePeekIcons.Coffee, stringResource(Res.string.favorites_roaster_shops),
                        roaster.coffeeShopsCount, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    FavoriteRoasterStat(CoffeePeekIcons.CoffeeBean, stringResource(Res.string.favorites_roaster_catalog),
                        roaster.coffeeProductsCount, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable private fun RoasterPlaceholder() {
    Icon(CoffeePeekIcons.Factory, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview @Composable private fun FavoriteRoasterCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoriteRoasterCard(FavoriteRoaster("sample", "Обжарщик", "sample", coffeeShopsCount = 7, coffeeProductsCount = 12), false, {}, {})
}
@Preview @Composable private fun FavoriteRoasterCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoriteRoasterCard(FavoriteRoaster("sample", "Обжарщик с длинным названием без публичного адреса"), true, {}, {})
}
