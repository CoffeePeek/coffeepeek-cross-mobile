package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_finish_zone
import com.coffeepeek.feature.shelf.impl.resources.shelf_finish_zone_hint
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ZoneShape = RoundedCornerShape(CoffeePeekDimens.radius2xl)

/**
 * Drop target shown while a bag is lifted, as in the shelf spec: a red-tinted block with a dashed red border, the archive box,
 * "Закончилась" and the hint. When the bag is over it ([highlighted]) the tint deepens, the border turns solid, the block grows
 * a little and the phone ticks, so letting go there is a clear choice.
 */
@Composable
internal fun ShelfFinishZone(highlighted: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(highlighted) { if (highlighted) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }
    // The spec's 14 % of the warning red over the screen background; deeper while the bag is over the zone.
    val tint = if (highlighted) 0.26f else 0.14f
    val fill by animateColorAsState(colors.error.copy(alpha = tint).compositeOver(colors.background), label = "zone fill")
    val scale by animateFloatAsState(if (highlighted) 1.04f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "zone scale")
    Column(modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.background(fill, ZoneShape)
        .drawBehind {
            val radius = CoffeePeekDimens.radius2xl.toPx()
            val dash = if (highlighted) null else PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
            drawRoundRect(colors.error, cornerRadius = CornerRadius(radius), style = Stroke(2.dp.toPx(), pathEffect = dash))
        }
        .heightIn(min = 88.dp).padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3)
        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        Icon(CoffeePeekIcons.Archive, null, Modifier.size(24.dp), tint = colors.error)
        Text(stringResource(Res.string.shelf_finish_zone), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold,
            color = colors.error)
        Text(stringResource(Res.string.shelf_finish_zone_hint), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShelfFinishZonePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        ShelfFinishZone(highlighted = false)
        ShelfFinishZone(highlighted = true)
    }
}

@Preview @Composable private fun ShelfFinishZoneLightPreview() = ShelfFinishZonePreview(false)
@Preview @Composable private fun ShelfFinishZoneDarkPreview() = ShelfFinishZonePreview(true)
