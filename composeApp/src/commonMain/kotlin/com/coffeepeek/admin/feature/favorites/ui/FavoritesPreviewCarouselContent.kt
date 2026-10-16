package com.coffeepeek.admin.feature.favorites.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens

@Composable
internal fun <T> FavoritesPreviewCarouselContent(
    coffeeShops: List<T>,
    roasters: List<T>,
    drinks: List<T>,
    itemKey: (T) -> String,
    card: @Composable (T, Modifier) -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth().testTag("favorites-preview-carousel")
            .horizontalScroll(rememberScrollState()),
        content = {
            listOf("shop" to coffeeShops, "roaster" to roasters, "drink" to drinks).forEach { (kind, items) ->
                items.forEach { item ->
                    key(kind, itemKey(item)) {
                        Box(Modifier.testTag("favorites-preview-$kind:${itemKey(item)}")) {
                            card(item, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val cardWidth = 300.dp.roundToPx()
        val gap = CpDimens.spacing3.roundToPx()
        val naturalConstraints = Constraints.fixedWidth(cardWidth)
        val shops = measurables.take(coffeeShops.size).map { it.measure(naturalConstraints) }
        // The last café is the one directly beside the first roaster stack. Its card
        // is measured naturally once, before giving the roasters their constraints.
        val referenceHeight = shops.lastOrNull()?.height
        val availableStackHeight = referenceHeight?.let { (it - gap).coerceAtLeast(0) }
        val roasterCards = measurables.drop(coffeeShops.size).take(roasters.size).mapIndexed { index, measurable ->
            val height = availableStackHeight?.let { available ->
                if (index % 2 == 0) available / 2 else available - available / 2
            }
            measurable.measure(if (height != null) Constraints.fixed(cardWidth, height) else naturalConstraints)
        }
        val roasterStacks = roasterCards.chunked(2)
        val drinkCards = measurables.drop(coffeeShops.size + roasters.size).map { it.measure(naturalConstraints) }
        val columnCount = shops.size + roasterStacks.size + drinkCards.size
        val contentWidth = cardWidth * columnCount + gap * (columnCount - 1).coerceAtLeast(0)
        val stackHeight = roasterStacks.maxOfOrNull { stack -> stack.sumOf { it.height } + gap * (stack.size - 1) } ?: 0
        val contentHeight = maxOf(
            shops.maxOfOrNull { it.height } ?: 0,
            stackHeight,
            drinkCards.maxOfOrNull { it.height } ?: 0,
        )
        layout(constraints.constrainWidth(contentWidth), constraints.constrainHeight(contentHeight)) {
            var x = 0
            shops.forEach { shop ->
                shop.placeRelative(x, 0)
                x += cardWidth + gap
            }
            roasterStacks.forEach { stack ->
                var y = 0
                stack.forEach { roaster ->
                    roaster.placeRelative(x, y)
                    y += roaster.height + gap
                }
                x += cardWidth + gap
            }
            drinkCards.forEach { drink ->
                drink.placeRelative(x, 0)
                x += cardWidth + gap
            }
        }
    }
}
