package com.coffeepeek.feature.shelf.data.scan.imaging

import com.coffeepeek.feature.shelf.domain.model.PackQuality

/** Geometry signals of the fitted [front] against its subject mask (the grid copy the scorer judged). */
internal fun frontQuality(front: FrontFit, subject: BooleanArray, width: Int, height: Int): PackQuality {
    val scorer = front.scorer
    val quad = front.quad.scaled(scorer.scale)
    var polygon = 0
    var clutter = 0
    for (y in 0 until scorer.height) {
        val span = quad.rowSpan(y, scorer.width)
        for (x in 0 until scorer.width) {
            val i = y * scorer.width + x
            if (span != null && x in span) polygon++ else if (scorer.mask[i] && scorer.textured[i]) clutter++
        }
    }
    val border = (0 until width).flatMap { listOf(it, (height - 1) * width + it) } + (0 until height).flatMap { listOf(it * width, it * width + width - 1) }
    val (w, h) = front.quad.rectangle()
    return PackQuality(
        sideSupport = scorer.supports(quad).average(),
        aspect = w / h,
        angleDeviation = front.quad.angleDeviation(),
        quadShare = polygon.toDouble() / (scorer.width * scorer.height),
        borderTouch = border.count { subject[it] }.toDouble() / border.size,
        clutterOutside = clutter.toDouble() / maxOf(polygon, 1),
        repaintShare = 0.0,
        strongGlare = 0.0,
    )
}
