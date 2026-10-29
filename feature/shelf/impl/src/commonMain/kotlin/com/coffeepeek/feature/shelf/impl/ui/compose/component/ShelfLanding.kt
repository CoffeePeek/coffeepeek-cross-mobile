package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/** A card dropping into place: [fall] brings it down, [squash] then springs it like a cushion on its bottom edge. */
internal class ShelfLanding(val fall: Animatable<Float, AnimationVector1D>, val squash: Animatable<Float, AnimationVector1D>)

/** Plays once for [key] when [landing]; [onLanded] after the spring has settled. Not landing, the card simply stands. */
@Composable
internal fun rememberShelfLanding(key: Any, landing: Boolean, onLanded: () -> Unit = {}): ShelfLanding {
    val landed by rememberUpdatedState(onLanded)
    val state = remember(key) { ShelfLanding(Animatable(if (landing) 0f else 1f), Animatable(0f)) }
    LaunchedEffect(key, landing) {
        if (!landing || state.fall.value >= 1f) return@LaunchedEffect
        // Falls like a dropped thing (speeding up) and stops dead with its bottom in place.
        state.fall.animateTo(1f, tween(480, easing = Easing { it * it }))
        // The top keeps going a little: squashed by 10 % of the height and wider in the middle, then springs back.
        state.squash.animateTo(1f, tween(70, easing = LinearOutSlowInEasing))
        state.squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 450f))
        landed()
    }
    return state
}

/** Draws [landing]: [fallHeight] px above its place at the start, squashed around its bottom centre on impact. */
internal fun Modifier.shelfLanding(landing: ShelfLanding, fallHeight: Float): Modifier = graphicsLayer {
    val fall = landing.fall.value
    val squash = landing.squash.value
    translationY = -(1 - fall) * fallHeight
    alpha = (fall * 3).coerceIn(0f, 1f)
    transformOrigin = TransformOrigin(0.5f, 1f)
    scaleY = 1f - 0.1f * squash
    scaleX = 1f + 0.06f * squash
}
