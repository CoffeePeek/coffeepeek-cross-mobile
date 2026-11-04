package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Bound both axes before the poster requests its aspect ratio, including in short viewports. */
@Composable
internal fun CollectionPosterFrame(format: CollectionPosterFormat, maxPosterHeight: Dp,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.heightIn(max = maxPosterHeight), contentAlignment = Alignment.Center) {
        val ratio = format.width.toFloat() / format.height
        val width = minOf(maxWidth, maxHeight * ratio)
        Box(Modifier.width(width).aspectRatio(ratio)) { content() }
    }
}

@Composable
private fun CollectionPosterFrameSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CollectionPosterFrame(CollectionPosterFormat.Story, 300.dp, Modifier.width(360.dp)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
@Preview @Composable private fun CollectionPosterFrameLightPreview() = CollectionPosterFrameSample(false)
@Preview @Composable private fun CollectionPosterFrameDarkPreview() = CollectionPosterFrameSample(true)
