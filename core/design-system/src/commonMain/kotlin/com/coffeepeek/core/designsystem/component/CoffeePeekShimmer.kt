package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Decorative loading shape. The owning status supplies a localized accessibility announcement. */
@Composable
fun CoffeePeekShimmer(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(CoffeePeekDimens.radiusMd), animate: Boolean = true) {
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    val moving = animate && scale > 0f
    val phase = if (moving) coffeePeekShimmerPhase() else remember { mutableStateOf(0f) }
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
    Box(modifier.clip(shape).drawWithCache {
        onDrawBehind {
            drawRect(base)
            if (moving) {
                val center = size.width * phase.value
                drawRect(Brush.linearGradient(listOf(Color.Transparent, highlight, Color.Transparent),
                    Offset(center - size.width / 2f, 0f), Offset(center + size.width / 2f, size.height)))
            }
        }
    }.clearAndSetSemantics { })
}

@Composable
private fun coffeePeekShimmerPhase(): State<Float> = rememberInfiniteTransition(label = "coffeepeek-shimmer").animateFloat(
    initialValue = -1f, targetValue = 2f,
    animationSpec = infiniteRepeatable(tween(1_250, easing = LinearEasing)), label = "coffeepeek-shimmer-band",
)

@Composable
private fun CoffeePeekShimmerSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.width(280.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekShimmer(Modifier.fillMaxWidth().height(120.dp))
        CoffeePeekShimmer(Modifier.fillMaxWidth(0.7f).height(24.dp))
        CoffeePeekShimmer(Modifier.fillMaxWidth(0.9f).height(16.dp), animate = false)
    }
}
@Preview @Composable private fun CoffeePeekShimmerLightPreview() = CoffeePeekShimmerSample(false)
@Preview @Composable private fun CoffeePeekShimmerDarkPreview() = CoffeePeekShimmerSample(true)
