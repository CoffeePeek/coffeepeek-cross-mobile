package com.coffeepeek.admin.feature.catalog.ui

import com.coffeepeek.core.designsystem.component.CoffeePeekLikeIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeBadge

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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
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
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.RoasterDetails
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCardContent(
    shop: CoffeeShop,
    distance: String? = null,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showCatalogDetails: Boolean = true,
    showOpenStatus: Boolean = true,
    compact: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(enabled = shop.publicAddress != null, onClick = onClick),
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (compact) {
            CompactShopCardContent(shop, distance, onToggleFavorite, showOpenStatus)
            return@Card
        }
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
                        .padding(CpDimens.spacing1),
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
                    CatalogFavoriteControl(
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
                    if (showOpenStatus) OpenStatusBadge(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactShopCardContent(
    shop: CoffeeShop,
    distance: String?,
    onToggleFavorite: (() -> Unit)?,
    showOpenStatus: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val photo = shop.photoUrl
            if (!photo.isNullOrBlank()) CoffeeShopImage(
                photo, contentDescription = shop.title,
                modifier = Modifier.fillMaxSize(), placeholderLabelSize = 10.sp,
            ) else CoffeeShopPlaceholderImage(labelSize = 10.sp, contentDescription = "Фото ${shop.title} отсутствует")
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                CatalogFavoriteControl(shop.isFavorite, onToggleFavorite, overImage = true, compact = true)
            }
            shop.rating?.takeIf { it > 0 }?.let { rating ->
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CpIcons.StarFilled, null, Modifier.size(11.dp), tint = CpColor.Primary)
                    Text(formatOneDecimal(rating), color = Color.White, fontSize = 10.sp, lineHeight = 12.sp)
                    if (shop.reviewCount > 0) Text("(${shop.reviewCount})", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, lineHeight = 12.sp)
                }
            }
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    shop.title, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (showOpenStatus) {
                    val color = if (shop.isOpen) CpColor.Success else MaterialTheme.colorScheme.error
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = 0.14f))
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(color))
                        Text(if (shop.isOpen) "ОТКРЫТО" else "ЗАКРЫТО", color = color, fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            val location = listOfNotNull(shop.address?.takeIf(String::isNotBlank), distance?.let { "$it от вас" }).joinToString(" · ")
            if (location.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(CpIcons.Location, null, Modifier.size(10.dp), tint = CpColor.Primary)
                Text(location, fontSize = 10.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (shop.tags.isNotEmpty()) FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                shop.tags.take(2).forEach { tag ->
                    Text(
                        tag, modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 5.dp, vertical = 3.dp),
                        fontSize = 9.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(shopTypeLabel(shop.type), modifier = Modifier.weight(1f), fontSize = 10.sp, lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(CpIcons.ChevronRight, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val photo = roaster.photoUrl?.takeIf(String::isNotBlank)
        ?: details?.photos?.firstOrNull()?.fullUrl?.takeIf(String::isNotBlank)
    val description = roaster.description?.takeIf(String::isNotBlank)
        ?: details?.about?.takeIf(String::isNotBlank)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val exactHeight = if (constraints.hasBoundedHeight && constraints.minHeight == constraints.maxHeight) maxHeight else null
        Card(
            modifier = Modifier.fillMaxWidth()
                .clickable(enabled = roaster.address?.slug?.isNotBlank() == true, onClick = onClick),
            shape = RoundedCornerShape(CpDimens.radiusXl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Layout(
                modifier = Modifier.fillMaxWidth()
                    .then(if (exactHeight != null) Modifier.height(exactHeight) else Modifier)
                    .padding(CpDimens.spacing3),
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                    ) {
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
                        Box(Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    roaster.name, modifier = Modifier.fillMaxWidth().padding(end = 36.dp),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                if (description != null) Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-12).dp)) {
                                CatalogFavoriteControl(
                                    isFavorite = isFavorite, onClick = onToggleFavorite,
                                    enabled = !isFavoriteLoading && roaster.id.isNotBlank(),
                                )
                            }
                        }
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
                },
            ) { measurables, constraints ->
                // Reserve the footer before measuring the flexible summary. It remains
                // visible when the carousel supplies exactly half a café card's height.
                val footer = measurables[1].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val gap = CpDimens.spacing2.roundToPx()
                val summaryHeight = if (constraints.hasBoundedHeight) {
                    (constraints.maxHeight - footer.height - gap).coerceAtLeast(0)
                } else Constraints.Infinity
                val summary = measurables[0].measure(constraints.copy(minHeight = 0, maxHeight = summaryHeight))
                val height = constraints.constrainHeight(summary.height + gap + footer.height)
                layout(constraints.maxWidth, height) {
                    summary.placeRelative(0, 0)
                    footer.placeRelative(0, height - footer.height)
                }
            }
        }
    }
}

/** A null action displays state without exposing an inactive favorite button. */
@Composable
private fun CatalogFavoriteControl(
    isFavorite: Boolean,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    overImage: Boolean = false,
    compact: Boolean = false,
) {
    if (onClick != null) {
        FavoriteButton(isFavorite = isFavorite, onClick = onClick, enabled = enabled, overImage = overImage)
    } else {
        if (overImage) {
            CoffeePeekLikeBadge(
                selected = isFavorite,
                contentDescription = if (isFavorite) "В избранном" else "Не в избранном",
                modifier = Modifier.size(if (compact) 32.dp else 48.dp),
            )
        } else Box(
            modifier = Modifier.size(if (compact) 24.dp else 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            CoffeePeekLikeIcon(
                selected = isFavorite,
                contentDescription = if (isFavorite) "В избранном" else "Не в избранном",
                overImage = overImage,
                modifier = Modifier.size(24.dp),
            )
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

@Preview @Composable private fun ShopCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCardContent(previewShop, "7,5 км", {}, {}, Modifier.width(280.dp), showCatalogDetails = false)
}

@Preview @Composable private fun ShopCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCardContent(previewShop, "7,5 км", {}, {}, Modifier.width(280.dp), showCatalogDetails = false)
}

private val previewShop = CoffeeShop(
    id = "preview", title = "Кофейня", rating = 4.0, reviewCount = 1,
    cityName = null, priceRange = null, photoUrl = null,
    address = "Кофейная улица, 1", isOpen = true, isFavorite = true,
)
