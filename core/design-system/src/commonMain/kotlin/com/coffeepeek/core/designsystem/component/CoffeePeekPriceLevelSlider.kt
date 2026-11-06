package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/** Null means any; labels and monetary thresholds belong to the feature, not this visual control. */
@Composable
fun CoffeePeekPriceLevelSlider(selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier,
    title: String? = null, hint: String? = null) {
    val filled = (selected ?: 0).coerceIn(0, 3)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        PriceLevelTrack(filled, onSelect)
        PriceLevelStops(filled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceLevelTrack(value: Int, onSelect: (Int?) -> Unit) {
    val colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = .4f), activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent)
    Slider(value = value.toFloat(), onValueChange = { onSelect(it.roundToInt().coerceIn(0, 3).takeIf { level -> level > 0 }) },
        valueRange = 0f..3f, steps = 2, colors = colors, interactionSource = remember { MutableInteractionSource() },
        thumb = { Box(Modifier.size(28.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape)) },
        track = { SliderDefaults.Track(it, Modifier.height(4.dp), colors = colors,
            drawStopIndicator = null, thumbTrackGapSize = 0.dp) })
}

@Composable
private fun PriceLevelStops(filled: Int) {
    val iconWidth = 14.dp * .9f
    BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp * (446.4f / 360.67f))) {
        val trackInset = 14.dp
        val trackWidth = (maxWidth - trackInset * 2).coerceAtLeast(0.dp)
        repeat(3) { index ->
            val tier = index + 1
            val groupWidth = iconWidth * tier + 1.dp * (tier - 1)
            val tickCenter = trackInset + trackWidth * (tier.toFloat() / 3)
            val x = (tickCenter - groupWidth / 2).coerceIn(0.dp, (maxWidth - groupWidth).coerceAtLeast(0.dp))
            CoffeePeekBynPriceIndicators(tier, Modifier.offset(x = x), iconWidth = iconWidth, spacing = 1.dp,
                tint = if (filled == tier) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable private fun PriceLevelSliderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.padding(16.dp)) {
        CoffeePeekPriceLevelSlider(null, {})
        CoffeePeekPriceLevelSlider(2, {})
    }
}
@Preview @Composable private fun CoffeePeekPriceLevelSliderLightPreview() = PriceLevelSliderPreview(false)
@Preview @Composable private fun CoffeePeekPriceLevelSliderDarkPreview() = PriceLevelSliderPreview(true)
