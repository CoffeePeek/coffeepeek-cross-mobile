package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Full cards, paired cards matching the adjacent full card's height, then full cards. */
@Composable
fun <T> CoffeePeekStackedCardCarousel(
    leading: List<T>,
    stacked: List<T>,
    trailing: List<T>,
    itemKey: (T) -> String,
    modifier: Modifier = Modifier,
    card: @Composable (T, Modifier) -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4),
        content = {
            listOf(leading, stacked, trailing).forEachIndexed { group, items ->
                items.forEach { item ->
                    key(group, itemKey(item)) { Box { card(item, Modifier.fillMaxSize()) } }
                }
            }
        },
    ) { measurables, constraints ->
        val width = 300.dp.roundToPx()
        val gap = CoffeePeekDimens.spacing3.roundToPx()
        val full = measurables.take(leading.size).map { it.measure(Constraints.fixedWidth(width)) }
        val pairs = measureCarouselPairs(measurables.drop(leading.size).take(stacked.size), width, gap, full.lastOrNull()?.height)
        val last = measurables.drop(leading.size + stacked.size).map { it.measure(Constraints.fixedWidth(width)) }
        val columns = full.map { listOf(it) } + pairs.chunked(2) + last.map { listOf(it) }
        val contentWidth = width * columns.size + gap * (columns.size - 1).coerceAtLeast(0)
        val contentHeight = columns.maxOfOrNull { column -> column.sumOf { it.height } + gap * (column.size - 1) } ?: 0
        layout(constraints.constrainWidth(contentWidth), constraints.constrainHeight(contentHeight)) {
            placeCarouselColumns(columns, width, gap)
        }
    }
}

private fun measureCarouselPairs(items: List<Measurable>, width: Int, gap: Int, referenceHeight: Int?): List<Placeable> =
    items.mapIndexed { index, item ->
        val available = referenceHeight?.let { (it - gap).coerceAtLeast(0) }
        val height = available?.let { if (index % 2 == 0) it / 2 else it - it / 2 }
        item.measure(if (height == null) Constraints.fixedWidth(width) else Constraints.fixed(width, height))
    }

private fun Placeable.PlacementScope.placeCarouselColumns(columns: List<List<Placeable>>, width: Int, gap: Int) {
    var x = 0
    columns.forEach { column ->
        var y = 0
        column.forEach { item ->
            item.placeRelative(x, y)
            y += item.height + gap
        }
        x += width + gap
    }
}

@Composable
private fun StackedCardCarouselPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekStackedCardCarousel(listOf("Большая карточка"), listOf("Первая", "Вторая"), listOf("Последняя"), { it }) { text, modifier ->
        Box(modifier.height(160.dp).padding(1.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center) { Text(text) }
    }
}

@Preview @Composable private fun StackedCardCarouselLightPreview() = StackedCardCarouselPreview(false)
@Preview @Composable private fun StackedCardCarouselDarkPreview() = StackedCardCarouselPreview(true)
