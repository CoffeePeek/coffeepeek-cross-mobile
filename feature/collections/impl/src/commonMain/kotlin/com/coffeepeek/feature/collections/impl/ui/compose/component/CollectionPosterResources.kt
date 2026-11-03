package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.collections.domain.model.SharedCollection
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

internal data class CollectionPosterLabels(val shop: String, val roaster: String, val qrHint: String,
    val count: String, val pages: List<String>, val checkIns: Map<String, String>)

internal data class CollectionPosterIcons(val shop: Painter, val roaster: Painter, val location: Painter,
    val star: Painter, val chevron: Painter)

internal data class CollectionPosterResources(val labels: CollectionPosterLabels, val icons: CollectionPosterIcons,
    val measurer: TextMeasurer, val typography: TextStyle)

@Composable
internal fun collectionPosterResources(collection: SharedCollection?, format: CollectionPosterFormat): CollectionPosterResources {
    val count = collection?.places?.size ?: 0
    val pages = ((count + format.perPage - 1) / format.perPage).coerceAtLeast(1)
    val checkIns = collection?.places.orEmpty().mapNotNull { place ->
        place.checkInCount?.let { place.key to collectionCheckInLabel(it) }
    }.toMap()
    val labels = CollectionPosterLabels(stringResource(Res.string.collections_shop), stringResource(Res.string.collections_roaster),
        stringResource(Res.string.collections_qr_hint), stringResource(Res.string.collections_count, count),
        (1..pages).map { stringResource(Res.string.collections_page, it, pages) }, checkIns)
    val icons = CollectionPosterIcons(rememberVectorPainter(CoffeePeekIcons.Coffee), rememberVectorPainter(CoffeePeekIcons.Factory),
        rememberVectorPainter(CoffeePeekIcons.Location), rememberVectorPainter(CoffeePeekIcons.StarFilled), rememberVectorPainter(CoffeePeekIcons.ChevronRight))
    return CollectionPosterResources(labels, icons, rememberTextMeasurer(cacheSize = 0), MaterialTheme.typography.bodyLarge)
}

@Composable
private fun collectionCheckInLabel(count: Int): String = stringResource(when {
    count % 100 in 11..14 -> Res.string.collections_checkins_many
    count % 10 == 1 -> Res.string.collections_checkins_one
    count % 10 in 2..4 -> Res.string.collections_checkins_few
    else -> Res.string.collections_checkins_many
}, count)
