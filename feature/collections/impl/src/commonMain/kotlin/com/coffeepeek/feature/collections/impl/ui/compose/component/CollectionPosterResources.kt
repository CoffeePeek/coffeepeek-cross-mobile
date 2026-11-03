package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.collections.domain.model.SharedCollection
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

internal data class CollectionPosterLabels(val shop: String, val roaster: String, val brand: String,
    val count: String, val pages: List<String>, val checkIns: Map<String, String>)
internal data class CollectionPosterIcons(val shop: Painter, val roaster: Painter, val location: Painter,
    val star: Painter, val chevron: Painter)
internal data class CollectionPosterResources(val labels: CollectionPosterLabels, val icons: CollectionPosterIcons,
    val measurer: TextMeasurer, val typography: TextStyle, val layout: CollectionPosterLayout)

@Composable
internal fun collectionPosterResources(collection: SharedCollection?, format: CollectionPosterFormat): CollectionPosterResources {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val typography = MaterialTheme.typography.bodyLarge
    val layout = remember(collection, format, measurer, typography) { posterLayout(collection, format, measurer, typography) }
    val count = collection?.places?.size ?: 0
    val pages = layout.pages.size.coerceAtLeast(1)
    val checkIns = collection?.places.orEmpty().mapNotNull { place ->
        place.checkInCount?.let { place.key to collectionCheckInLabel(it) }
    }.toMap()
    val labels = CollectionPosterLabels(stringResource(Res.string.collections_shop), stringResource(Res.string.collections_roaster),
        stringResource(Res.string.collections_brand),
        stringResource(Res.string.collections_count, count),
        (1..pages).map { stringResource(Res.string.collections_page, it, pages) }, checkIns)
    val icons = CollectionPosterIcons(rememberVectorPainter(CoffeePeekIcons.Coffee), rememberVectorPainter(CoffeePeekIcons.Factory),
        rememberVectorPainter(CoffeePeekIcons.Location), rememberVectorPainter(CoffeePeekIcons.StarFilled), rememberVectorPainter(CoffeePeekIcons.ChevronRight))
    return CollectionPosterResources(labels, icons, measurer, typography, layout)
}

private fun posterLayout(collection: SharedCollection?, format: CollectionPosterFormat,
    measurer: TextMeasurer, typography: TextStyle): CollectionPosterLayout {
    val title = measureCollectionPosterText(measurer, typography, collection?.title.orEmpty(), 1016f, 44, FontWeight.Bold, 2)
    return collectionPosterLayout(collection?.places.orEmpty(), format, 128f + title.size.height) { place ->
        196f + measureCollectionPosterText(measurer, typography, place.title, 1016f, 30, FontWeight.Bold).size.width
    }
}

internal fun measureCollectionPosterText(measurer: TextMeasurer, typography: TextStyle, value: String, width: Float,
    fontSize: Int, weight: FontWeight = FontWeight.Normal, lines: Int = 1, align: TextAlign = TextAlign.Start): TextLayoutResult {
    val pixels = width.toInt().coerceAtLeast(1)
    return measurer.measure(AnnotatedString(value), style = typography.copy(fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.12f).sp, fontWeight = weight, letterSpacing = 0.sp, textAlign = align),
        maxLines = lines, overflow = TextOverflow.Ellipsis,
        constraints = Constraints(minWidth = if (align == TextAlign.End) pixels else 0, maxWidth = pixels), density = Density(1f))
}

@Composable
private fun collectionCheckInLabel(count: Int): String = stringResource(when {
    count % 100 in 11..14 -> Res.string.collections_checkins_many
    count % 10 == 1 -> Res.string.collections_checkins_one
    count % 10 in 2..4 -> Res.string.collections_checkins_few
    else -> Res.string.collections_checkins_many
}, count)
