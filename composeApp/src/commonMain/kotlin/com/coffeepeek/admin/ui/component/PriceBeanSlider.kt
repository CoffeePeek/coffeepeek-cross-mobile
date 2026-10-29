package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.DpSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.ic_byn_symbol
import com.coffeepeek.admin.theme.CpColor
import org.jetbrains.compose.resources.painterResource

private const val MAX_PRICE_LEVEL = 3
private const val BYN_ICON_ASPECT = 360.67f / 446.4f
private const val BYN_ICON_SCALE = 0.9f

private fun Modifier.bynIconSize(width: Dp): Modifier {
    val scaledWidth = width * BYN_ICON_SCALE
    return size(DpSize(scaledWidth, scaledWidth / BYN_ICON_ASPECT))
}

/** Parses domain price labels (`$`…`$$$$`) into level 1–3 for UI. */
fun priceRangeLevel(priceRange: String?): Int? {
    val level = priceRange?.count { it == '$' } ?: return null
    return level.coerceIn(1, 4).coerceAtMost(MAX_PRICE_LEVEL)
}

@Composable
fun PriceBynIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(Res.drawable.ic_byn_symbol),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
fun PriceBynRow(
    level: Int,
    modifier: Modifier = Modifier,
    maxLevel: Int = MAX_PRICE_LEVEL,
    iconSize: Dp = 14.dp,
    activeTint: Color = CpColor.Primary,
    inactiveTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(1.dp),
) {
    val filled = level.coerceIn(0, maxLevel)
    if (filled <= 0) return
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(filled) {
            PriceBynIcon(
                modifier = Modifier.bynIconSize(iconSize),
                tint = activeTint,
            )
        }
    }
}

/** @deprecated Use [PriceBynRow]. */
@Composable
fun PriceBeansRow(
    level: Int,
    modifier: Modifier = Modifier,
    maxLevel: Int = MAX_PRICE_LEVEL,
    iconSize: Dp = 14.dp,
    activeTint: Color = CpColor.Primary,
    inactiveTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    showEmptySlots: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(2.dp),
) {
    PriceBynRow(
        level = level,
        modifier = modifier,
        maxLevel = maxLevel,
        iconSize = iconSize,
        activeTint = activeTint,
        inactiveTint = inactiveTint,
        horizontalArrangement = horizontalArrangement,
    )
}

/**
 * Discrete price filter: null/0 = any, 1–3 = BYN tiers (как в макете).
 */
@Composable
fun PriceBeanSlider(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Цена",
    showTitle: Boolean = true,
    showHint: Boolean = true,
) {
    com.coffeepeek.core.designsystem.component.CoffeePeekPriceLevelSlider(
        selected, onSelect, modifier,
        title = title.takeIf { showTitle },
        hint = priceLevelHint(selected).takeIf { showHint },
    )
}

fun priceLevelHint(selected: Int?): String? = when (selected) {
    1 -> "До 8 BYN"
    2 -> "Около 8 BYN"
    3 -> "Больше 8 BYN"
    else -> null
}

fun priceLevelValue(selected: Int?): String? = when (selected) {
    1 -> "< 8 BYN"
    2 -> "8 BYN"
    3 -> "> 8 BYN"
    else -> null
}
