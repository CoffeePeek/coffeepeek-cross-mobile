package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpColor
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_distance_from_you
import com.coffeepeek.feature.favorites.impl.resources.favorites_remove_description
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_logo_description
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource

/** Feature-owned rendering: never imports feed UI or legacy shop aggregates. */
@Composable
internal fun FavoriteCard(
    shop: FavoriteShop,
    removing: Boolean,
    distance: String?,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen),
        shape = RoundedCornerShape(CpDimens.radiusXl),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                FavoritePhoto(shop.photoUrl, shop.title, Modifier.fillMaxSize())

                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(CpDimens.spacing3),
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    shop.rating?.takeIf { it.isFinite() && it > 0 }?.let { rating ->
                        RatingBadge(rating, shop.reviewCount)
                    }
                    Box(
                        // Keep the full interactive target comfortably usable by touch and
                        // assistive technology; the icon itself remains visually compact.
                        modifier = Modifier.size(48.dp)
                            .clip(RoundedCornerShape(CpDimens.radiusLg))
                            .background(Color.Black.copy(alpha = 0.68f))
                            .clickable(enabled = !removing, role = Role.Button, onClick = onRemove),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            CpIcons.FavoriteFilled,
                            contentDescription = stringResource(
                                Res.string.favorites_remove_description, shop.title,
                            ),
                            tint = CpColor.Error,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                val logos = shop.roasterPhotoUrls.filter(String::isNotBlank).distinct().take(3)
                if (logos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(CpDimens.spacing3),
                        horizontalArrangement = Arrangement.spacedBy((-14).dp),
                    ) {
                        logos.forEachIndexed { index, url ->
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                KamelImage(
                                    resource = { asyncPainterResource(url) },
                                    contentDescription = stringResource(
                                        Res.string.favorites_roaster_logo_description, index + 1,
                                    ),
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onLoading = { Icon(CpIcons.Coffee, null, Modifier.size(18.dp)) },
                                    onFailure = { Icon(CpIcons.Coffee, null, Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = CpDimens.spacing4, top = CpDimens.spacing3,
                    end = CpDimens.spacing4, bottom = CpDimens.spacing4,
                ),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shop.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OpenStatusBadge(shop.isOpen, Modifier.padding(start = CpDimens.spacing2))
                }

                if (shop.brewMethods.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                        shop.brewMethods.take(2).forEach { method -> InfoChip(method) }
                        if (shop.brewMethods.size > 2) InfoChip("+${shop.brewMethods.size - 2}")
                    }
                }

                val location = listOfNotNull(
                    shop.address?.takeIf(String::isNotBlank)
                        ?: shop.cityName?.takeIf(String::isNotBlank),
                    distance?.let { stringResource(Res.string.favorites_distance_from_you, it) },
                ).joinToString(" • ")
                if (location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(CpIcons.Location, null, tint = CpColor.Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        shop.priceRange?.takeIf(String::isNotBlank)?.let { price ->
                            Text(price, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        shop.tags.firstOrNull { it.isNotBlank() && it !in shop.brewMethods }?.let { tag ->
                            if (!shop.priceRange.isNullOrBlank()) Text("·", style = MaterialTheme.typography.labelSmall)
                            Text(tag, modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Icon(CpIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Preview @Composable private fun FavoriteCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoriteCard(previewFavoriteShop, false, "1 км", {}, {})
}
@Preview @Composable private fun FavoriteCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoriteCard(previewFavoriteShop.copy(isOpen = false), false, "1 км", {}, {})
}

private val previewFavoriteShop = FavoriteShop(
    id = "sample", title = "Любимая кофейня", rating = 4.8, reviewCount = 12,
    address = "Улица Кофейная, 1", isOpen = true, priceRange = "\$\$",
    tags = listOf("Спешелти"), brewMethods = listOf("Эспрессо", "Фильтр"),
)
