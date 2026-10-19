package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.R
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterImage

/**
 * Catalogue card. Geometry is the legacy one: when the carousel supplies an exact height, the footer is measured
 * first so the stats stay visible and the summary shrinks.
 */
@Composable
internal fun RoasterCard(
    name: String,
    photoUrl: String?,
    description: String?,
    shopsCount: Int,
    productsCount: Int,
    isFavorite: Boolean,
    onOpen: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
    image: RoasterImage,
    modifier: Modifier = Modifier,
) {
    val photo = photoUrl?.takeIf(String::isNotBlank)
    val summary = description?.takeIf(String::isNotBlank)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val exactHeight = if (constraints.hasBoundedHeight && constraints.minHeight == constraints.maxHeight) maxHeight else null
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = onOpen != null, onClick = { onOpen?.invoke() }),
            shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Layout(
                modifier = Modifier.fillMaxWidth()
                    .then(if (exactHeight != null) Modifier.height(exactHeight) else Modifier)
                    .padding(CoffeePeekDimens.spacing3),
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
                                .background(if (photo != null) Color.White else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photo != null) {
                                image(photo, stringResource(R.string.roaster_logo, name), Modifier.fillMaxSize(), ContentScale.Fit, 6.sp)
                            } else Icon(CoffeePeekIcons.Factory, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(Modifier.weight(1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(name, modifier = Modifier.fillMaxWidth().padding(end = 36.dp),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (summary != null) Text(summary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            if (onToggleFavorite != null) Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-12).dp)) {
                                RoasterFavoriteHeart(isFavorite, onToggleFavorite)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RoasterStat(CoffeePeekIcons.Coffee, stringResource(R.string.roaster_stat_shops), shopsCount, Modifier.weight(1f))
                        VerticalDivider(Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        RoasterStat(CoffeePeekIcons.CoffeeBean, stringResource(R.string.roaster_stat_catalog), productsCount, Modifier.weight(1f))
                    }
                },
            ) { measurables, constraints ->
                // Reserve the footer before measuring the flexible summary.
                val footer = measurables[1].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val gap = CoffeePeekDimens.spacing2.roundToPx()
                val summaryHeight = if (constraints.hasBoundedHeight) {
                    (constraints.maxHeight - footer.height - gap).coerceAtLeast(0)
                } else Constraints.Infinity
                val body = measurables[0].measure(constraints.copy(minHeight = 0, maxHeight = summaryHeight))
                val height = constraints.constrainHeight(body.height + gap + footer.height)
                layout(constraints.maxWidth, height) {
                    body.placeRelative(0, 0)
                    footer.placeRelative(0, height - footer.height)
                }
            }
        }
    }
}

@PreviewLightDark @Composable private fun RoasterCardPreview() = CoffeePeekTheme {
    RoasterCard("Sandbox Roaster", null, "Локальный обжарщик для тестирования", 7, 21, isFavorite = true,
        onOpen = {}, onToggleFavorite = {}, image = { _, _, _, _, _ -> })
}

@PreviewLightDark @Composable private fun RoasterCardEmptyStatsPreview() = CoffeePeekTheme {
    RoasterCard("Очень длинное название обжарщика для проверки обрезки строки", "https://x", null, 0, 0, isFavorite = false,
        onOpen = null, onToggleFavorite = null, image = { _, _, _, _, _ -> })
}
