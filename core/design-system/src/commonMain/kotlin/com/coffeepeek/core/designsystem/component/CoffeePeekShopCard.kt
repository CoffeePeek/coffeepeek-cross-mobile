package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.core.designsystem.theme.coffeePeekStatusForeground
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val MAX_LOGOS = 3
private val LogoSize = 36.dp
private val LogoStep = 22.dp

/**
 * The coffee shop card of the catalogue, the discovery row and the search results. It knows no feature models and loads no images: the cover, the roaster logos,
 * the brew method chips and the favorite control are slots, the texts come from the caller (so they are localized there).
 *
 * @param rating a badge over the cover with the value and, when [ratingCount] is above zero, the count; none without a rating.
 * @param statusLabel the open/closed pill beside the title (null hides it), green when [isOpen], red otherwise.
 * @param kindLabel the kind of the place, followed by the price marks ([priceLevel]) and the first [tag] when there are any.
 * @param roasterLogoCount at most three overlapping logos at the bottom end of the cover; [roasterLogo] draws the one of an index.
 * @param favorite placed at the top end of the cover, next to the rating.
 * A null [onClick] makes a read-only card.
 */
@Composable
fun CoffeePeekShopCard(
    title: String,
    kindLabel: String,
    modifier: Modifier = Modifier,
    rating: Double? = null,
    ratingText: String = "",
    ratingCount: Int = 0,
    isNew: Boolean = false,
    newLabel: String = "",
    statusLabel: String? = null,
    isOpen: Boolean = false,
    locationSummary: String? = null,
    priceLevel: Int? = null,
    tag: String? = null,
    onClick: (() -> Unit)? = null,
    cover: @Composable BoxScope.() -> Unit,
    roasterLogoCount: Int = 0,
    roasterLogo: @Composable BoxScope.(Int) -> Unit = {},
    brewMethods: (@Composable () -> Unit)? = null,
    favorite: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() }),
        shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(2f)
                    .clip(RoundedCornerShape(topStart = CoffeePeekDimens.radiusXl, topEnd = CoffeePeekDimens.radiusXl))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                cover()
                if (isNew) NewBadge(newLabel, Modifier.align(Alignment.TopStart).padding(CoffeePeekDimens.spacing3))
                Row(Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing1), verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null && rating > 0) RatingBadge(ratingText, ratingCount)
                    favorite?.invoke()
                }
                val logos = roasterLogoCount.coerceIn(0, MAX_LOGOS)
                if (logos > 0) Box(Modifier.align(Alignment.BottomEnd).padding(CoffeePeekDimens.spacing3).width(LogoSize + LogoStep * (logos - 1)).height(LogoSize)) {
                    repeat(logos) { index ->
                        Box(
                            Modifier.offset(x = LogoStep * index).size(LogoSize).clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { roasterLogo(index) }
                    }
                }
            }
            Column(
                Modifier.padding(start = CoffeePeekDimens.spacing4, top = CoffeePeekDimens.spacing3, end = CoffeePeekDimens.spacing4, bottom = CoffeePeekDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    if (statusLabel != null) StatusBadge(statusLabel, isOpen, Modifier.padding(start = CoffeePeekDimens.spacing2))
                }
                brewMethods?.invoke()
                if (!locationSummary.isNullOrBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(CoffeePeekIcons.Location, contentDescription = null, tint = MaterialTheme.colorScheme.coffeePeekAccentForeground, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(locationSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(kindLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (priceLevel != null && priceLevel > 0) {
                            Separator()
                            // Prod proportions: the symbol drawn at 90% of 8 dp, one dp apart.
                            CoffeePeekBynPriceIndicators(priceLevel, iconWidth = 7.2.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, spacing = 1.dp)
                        }
                        if (!tag.isNullOrBlank()) {
                            Separator()
                            Text(
                                tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                    Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** A small rounded chip with an optional icon, for the brew methods of a card. */
@Composable
fun CoffeePeekShopInfoChip(text: String, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusLg)
    Row(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = CoffeePeekDimens.spacing2, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NewBadge(label: String, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusLg)).background(CoffeePeekColors.DarkSurface.copy(alpha = 0.92f))
            .padding(horizontal = CoffeePeekDimens.spacing3, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
    ) {
        Icon(CoffeePeekIcons.Sparkle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RatingBadge(text: String, count: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusLg)).background(Color.Black.copy(alpha = 0.68f)).padding(horizontal = CoffeePeekDimens.spacing2, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CoffeePeekIcons.StarFilled, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        if (count > 0) Text("($count)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
    }
}

@Composable
private fun StatusBadge(label: String, isOpen: Boolean, modifier: Modifier) {
    val color = if (isOpen) CoffeePeekColors.Success else MaterialTheme.colorScheme.error
    val foreground = MaterialTheme.colorScheme.coffeePeekStatusForeground(isOpen)
    Row(
        modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusSm)).background(color.copy(alpha = 0.14f)).padding(horizontal = CoffeePeekDimens.spacing2, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(foreground))
        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Separator() = Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun ShopCardPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    androidx.compose.material3.Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CoffeePeekShopCard(
                title = "Sandbox Cafe", kindLabel = "Specialty", rating = 4.5, ratingText = "4.5", ratingCount = 12, isNew = true, newLabel = "Новое",
                statusLabel = "ОТКРЫТО", isOpen = true, locationSummary = "Тестовая улица, 1 • 250 м от вас", priceLevel = 2, tag = "Wi-Fi", onClick = {},
                cover = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))) }, roasterLogoCount = 2,
                roasterLogo = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outline)) },
                brewMethods = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { CoffeePeekShopInfoChip("V60"); CoffeePeekShopInfoChip("+1") } },
                favorite = { CoffeePeekLikeBadge(selected = true, contentDescription = "В избранном", modifier = Modifier.size(48.dp)) },
            )
            CoffeePeekShopCard(
                title = "Закрытая кофейня с очень длинным названием для проверки обрезки", kindLabel = "Кофейня", statusLabel = "ЗАКРЫТО",
                cover = { Box(Modifier.fillMaxSize()) },
            )
        }
    }
}

@Preview @Composable private fun CoffeePeekShopCardLightPreview() = ShopCardPreviewContent(darkTheme = false)
@Preview @Composable private fun CoffeePeekShopCardDarkPreview() = ShopCardPreviewContent(darkTheme = true)
