package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.BagScanFrame
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfStageOutcome
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfStageStatus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A scan that should not become a card: the photo with what the scanner found, how each stage went and how to fix the
 * failed ones. [failure] adds why a scan could not run at all.
 */
@Composable
internal fun ShelfScanRetake(
    photo: ByteArray, frame: BagScanFrame?, outcomes: List<ShelfStageOutcome>, failure: BagScanIssue?,
    onCamera: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        ShelfScanPhoto(photo, frame, Modifier.fillMaxWidth().weight(1f), frameColor = MaterialTheme.colorScheme.error)
        Text(stringResource(Res.string.shelf_retake_title), Modifier.semantics { heading() }.padding(top = CoffeePeekDimens.spacing2),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
        if (failure != null) Text(failure.text(), style = MaterialTheme.typography.bodyLarge)
        ShelfScanStagesResult(outcomes)
        val tips = outcomes.filter { it.status == ShelfStageStatus.Failed }.flatMap { it.reasons }.take(2).mapNotNull { it.tip() }
        tips.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        CoffeePeekButton(stringResource(Res.string.shelf_retake_camera), onCamera, Modifier.padding(top = CoffeePeekDimens.spacing2),
            leadingIcon = { Icon(CoffeePeekIcons.Camera, null) })
        ShelfSecondaryButton(stringResource(Res.string.shelf_retake_gallery), onGallery)
    }
}

@Composable
private fun ShelfScanRetakePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanRetake(byteArrayOf(), null, listOf(
        ShelfStageOutcome(BagScanStage.Segmentation, ShelfStageStatus.Done, emptyList()),
        ShelfStageOutcome(BagScanStage.Front, ShelfStageStatus.Done, emptyList()),
        ShelfStageOutcome(BagScanStage.Repair, ShelfStageStatus.Failed, listOf(PackModerationReason.ObjectsOnBag)),
        ShelfStageOutcome(BagScanStage.Soften, ShelfStageStatus.Done, emptyList()),
    ), null, {}, {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfScanRetakeLightPreview() = ShelfScanRetakePreview(false)
@Preview @Composable private fun ShelfScanRetakeDarkPreview() = ShelfScanRetakePreview(true)
