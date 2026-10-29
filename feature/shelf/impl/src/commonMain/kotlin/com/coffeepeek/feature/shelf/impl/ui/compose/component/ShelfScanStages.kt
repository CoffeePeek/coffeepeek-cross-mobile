package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfStageOutcome
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfStageStatus
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Stages while the scan runs. A finished stage gets a neutral tick: whether it went well is only known with the verdict,
 * so nothing here promises success.
 */
@Composable
internal fun ShelfScanStagesRunning(stagesDone: Set<BagScanStage>, modifier: Modifier = Modifier) {
    val current = BagScanStage.entries.firstOrNull { it !in stagesDone }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        BagScanStage.entries.forEach { stage ->
            when {
                stage in stagesDone -> ShelfStageRow(stage.text(passed = false), muted) { ShelfStageRing(muted, filled = true) }
                stage == current -> ShelfStageRow(stage.text(passed = false), MaterialTheme.colorScheme.onSurface) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                else -> ShelfStageRow(stage.text(passed = false), muted) { ShelfStageRing(MaterialTheme.colorScheme.outlineVariant, filled = false) }
            }
        }
    }
}

/** The verdict stage by stage: what passed, what failed and why, what only deserves advice, what was never reached. */
@Composable
internal fun ShelfScanStagesResult(outcomes: List<ShelfStageOutcome>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        outcomes.forEach { outcome ->
            val reasons = outcome.reasons.map { it.text() }
            when (outcome.status) {
                ShelfStageStatus.Done -> ShelfStageRow(outcome.stage.text(passed = true), colors.onSurface) {
                    ShelfStageRing(colors.primary, filled = true)
                }
                ShelfStageStatus.Warning -> ShelfStageRow(reasons.joinToString(". "), colors.onSurface) { ShelfStageIcon(CoffeePeekIcons.Info, colors.primary) }
                ShelfStageStatus.Failed -> ShelfStageRow(reasons.ifEmpty { listOf(outcome.stage.text(passed = false)) }.joinToString(". "),
                    colors.error) { ShelfStageIcon(CoffeePeekIcons.Error, colors.error) }
                ShelfStageStatus.Skipped -> ShelfStageRow(outcome.stage.text(passed = false), colors.onSurfaceVariant) {
                    ShelfStageRing(colors.outlineVariant, filled = false)
                }
            }
        }
    }
}

@Composable
private fun ShelfStageRow(text: String, color: Color, icon: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun ShelfStageIcon(icon: ImageVector, tint: Color) = Icon(icon, null, tint = tint)

/** A ring, with a dot inside once the stage has run. */
@Composable
private fun ShelfStageRing(color: Color, filled: Boolean) = Canvas(Modifier.size(16.dp)) {
    val stroke = 2.dp.toPx()
    drawCircle(color, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
    if (filled) drawCircle(color, radius = size.minDimension / 2 - 2 * stroke)
}

@Composable
private fun ShelfScanStagesPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
        ShelfScanStagesRunning(setOf(BagScanStage.Segmentation))
        ShelfScanStagesResult(listOf(
            ShelfStageOutcome(BagScanStage.Segmentation, ShelfStageStatus.Done, emptyList()),
            ShelfStageOutcome(BagScanStage.Front, ShelfStageStatus.Warning, listOf(PackModerationReason.Tilted)),
            ShelfStageOutcome(BagScanStage.Repair, ShelfStageStatus.Failed, listOf(PackModerationReason.ObjectsOnBag)),
            ShelfStageOutcome(BagScanStage.Soften, ShelfStageStatus.Skipped, emptyList()),
        ))
    }
}

@Preview @Composable private fun ShelfScanStagesLightPreview() = ShelfScanStagesPreview(false)
@Preview @Composable private fun ShelfScanStagesDarkPreview() = ShelfScanStagesPreview(true)
