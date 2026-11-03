package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/** Discrete integer stops with a thin track; the caller owns the domain labels and selected-value description. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeePeekDiscreteSlider(
    selected: Int, maximum: Int, onSelect: (Int) -> Unit,
    label: String, selectedDescription: String, modifier: Modifier = Modifier,
) {
    require(maximum > 0)
    val colors = SliderDefaults.colors(
        thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
        activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
    )
    Slider(
        value = selected.coerceIn(0, maximum).toFloat(),
        onValueChange = { onSelect(it.roundToInt().coerceIn(0, maximum)) },
        valueRange = 0f..maximum.toFloat(), steps = maximum - 1,
        modifier = modifier.semantics {
            contentDescription = label
            stateDescription = selectedDescription
        },
        colors = colors, interactionSource = remember { MutableInteractionSource() },
        thumb = { Box(Modifier.size(28.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape)) },
        track = { state ->
            SliderDefaults.Track(state, Modifier.height(4.dp), colors = colors,
                drawStopIndicator = null, thumbTrackGapSize = 0.dp)
        },
    )
}

@Composable
private fun CoffeePeekDiscreteSliderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePeekDiscreteSlider(2, 4, {}, "Price", "Moderate") }
}

@Preview @Composable private fun CoffeePeekDiscreteSliderLightPreview() = CoffeePeekDiscreteSliderPreview(false)
@Preview @Composable private fun CoffeePeekDiscreteSliderDarkPreview() = CoffeePeekDiscreteSliderPreview(true)
