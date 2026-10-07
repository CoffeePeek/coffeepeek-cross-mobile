package com.coffeepeek.admin.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.FavoriteButton
import com.coffeepeek.admin.ui.component.PriceBynRow
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.priceRangeLevel
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.RoasterDetails
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ShopCardContent(
    shop: CoffeeShop,
    distance: String? = null,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showCatalogDetails: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(enabled = shop.publicAddress != null, onClick = onClick),
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(topStart = CpDimens.radiusXl, topEnd = CpDimens.radiusXl))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val photoUrl = shop.photoUrl
                if (!photoUrl.isNullOrBlank()) {
                    CoffeeShopImage(
                        imageUrl = photoUrl,
                        contentDescription = shop.title,
                        contentScale = ContentScale.Crop,
                        placeholderLabelSize = 18.sp,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CoffeeShopPlaceholderImage(
                        labelSize = 18.sp,
                        contentDescription = "Фото ${shop.title} отсутствует",
                    )
                }
                if (shop.isNew) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(CpDimens.spacing3)
                            .clip(RoundedCornerShape(CpDimens.radiusLg))
                            .background(CpColor.DarkSurface.copy(alpha = 0.92f))
                            .padding(horizontal = CpDimens.spacing3, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                    ) {
                        Icon(
                            imageVector = CpIcons.Sparkle,
                            contentDescription = null,
                            tint = CpColor.Primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Новое",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CpDimens.spacing3),
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val rating = shop.rating
                    if (rating != null && rating > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CpDimens.radiusLg))
                                .background(Color.Black.copy(alpha = 0.68f))
                                .padding(horizontal = CpDimens.spacing2, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = CpIcons.StarFilled,
                                contentDescription = null,
                                tint = CpColor.Primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = formatOneDecimal(rating),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (shop.reviewCount > 0) {
                                Text(
                                    text = "(${shop.reviewCount})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f),
                                )
                            }
                        }
                    }
                    FavoriteButton(
                        isFavorite = shop.isFavorite,
                        onClick = onToggleFavorite,
                        overImage = true,
                    )
                }

                val visibleRoasterLogos = shop.roasterPhotoUrls
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(3)
                if (showCatalogDetails && visibleRoasterLogos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(CpDimens.spacing3)
                            .width(36.dp + 22.dp * (visibleRoasterLogos.size - 1))
                            .height(36.dp),
                    ) {
                        visibleRoasterLogos.forEachIndexed { index, logoUrl ->
                            Box(
                                modifier = Modifier
                                    .offset(x = 22.dp * index)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                CoffeeShopImage(
                                    imageUrl = logoUrl,
                                    contentDescription = "Логотип обжарщика ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    placeholderLabelSize = 5.sp,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = CpDimens.spacing4,
                    top = CpDimens.spacing3,
                    end = CpDimens.spacing4,
                    bottom = CpDimens.spacing4,
                ),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = shop.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OpenStatusBadge(
                        isOpen = shop.isOpen,
                        modifier = Modifier.padding(start = CpDimens.spacing2),
                    )
                }

                if (showCatalogDetails && shop.brewMethods.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                    ) {
                        shop.brewMethods.take(2).forEach { method ->
                            ShopInfoChip(text = method, brewIcon = brewMethodIcon(method))
                        }
                        if (shop.brewMethods.size > 2) {
                            ShopInfoChip(text = "+${shop.brewMethods.size - 2}")
                        }
                    }
                }

                val locationSummary = listOfNotNull(
                    shop.address?.takeIf { it.isNotBlank() },
                    distance?.let { "$it от вас" },
                ).joinToString(" • ")
                if (locationSummary.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CpIcons.Location,
                            contentDescription = null,
                            tint = CpColor.Primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = locationSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = shopTypeLabel(shop.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        priceRangeLevel(shop.priceRange)?.let { level ->
                            DetailSeparator()
                            PriceBynRow(
                                level = level,
                                iconSize = 8.dp,
                                activeTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        shop.tags.takeIf { showCatalogDetails }.orEmpty()
                            .filterNot { it in shop.brewMethods }
                            .firstOrNull()
                            ?.let { tag ->
                                DetailSeparator()
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                    }
                    Icon(
                        imageVector = CpIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenStatusBadge(isOpen: Boolean, modifier: Modifier = Modifier) {
    val color = if (isOpen) CpColor.Success else MaterialTheme.colorScheme.error
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CpDimens.radiusSm))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = CpDimens.spacing2, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Text(
            text = if (isOpen) "ОТКРЫТО" else "ЗАКРЫТО",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ShopInfoChip(
    text: String,
    brewIcon: DrawableResource? = null,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 112.dp)
            .clip(RoundedCornerShape(CpDimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(CpDimens.radiusLg),
            )
            .padding(horizontal = CpDimens.spacing2, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        brewIcon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun shopTypeLabel(type: String): String = when (type) {
    com.coffeepeek.domain.model.CoffeeShopType.SPECIALTY -> "Specialty"
    com.coffeepeek.domain.model.CoffeeShopType.CAFE -> "Кафе"
    else -> "Кофейня"
}

@Composable
internal fun RoasterCardContent(
    roaster: CatalogItem,
    details: RoasterDetails?,
    onClick: () -> Unit,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .clickable(enabled = roaster.address?.slug?.isNotBlank() == true, onClick = onClick),
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CpDimens.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            val photo = roaster.photoUrl?.takeIf(String::isNotBlank)
                ?: details?.photos?.firstOrNull()?.fullUrl?.takeIf(String::isNotBlank)
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(CpDimens.radiusMd))
                    .background(if (photo != null) Color.White else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) CoffeeShopImage(
                    imageUrl = photo, contentDescription = "Логотип ${roaster.name}",
                    contentScale = ContentScale.Fit, placeholderLabelSize = 6.sp,
                    modifier = Modifier.fillMaxSize(),
                ) else Icon(CpIcons.Factory, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        roaster.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    FavoriteButton(
                        isFavorite = isFavorite, onClick = onToggleFavorite,
                        enabled = !isFavoriteLoading && roaster.id.isNotBlank(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RoasterStat(CpIcons.Coffee, "Кофейни", roaster.coffeeShopsCount, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    RoasterStat(CpIcons.CoffeeBean, "Каталог", roaster.coffeeProductsCount, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RoasterStat(icon: ImageVector, label: String, count: Int, modifier: Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(roasterCountLabel(count), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun roasterCountLabel(count: Int): String = if (count > 0) count.toString() else "—"
