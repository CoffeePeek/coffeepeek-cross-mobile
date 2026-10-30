package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

@Composable
internal actual fun CoffeePeekPlatformLoader(contentDescription: String, modifier: Modifier, size: Dp, color: Color) {
    val rotation by rememberInfiniteTransition(label = "glass-loader").animateFloat(0f, 360f,
        infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "glass-loader-rotation")
    Canvas(modifier.size(size).semantics {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }) {
        repeat(12) { spoke ->
            rotate(rotation + spoke * 30f) {
                drawLine(color.copy(alpha = color.alpha * (0.2f + spoke / 11f * 0.8f)),
                    Offset(center.x, this.size.height * 0.12f), Offset(center.x, this.size.height * 0.3f),
                    strokeWidth = this.size.minDimension * 0.08f, cap = StrokeCap.Round)
            }
        }
    }
}
