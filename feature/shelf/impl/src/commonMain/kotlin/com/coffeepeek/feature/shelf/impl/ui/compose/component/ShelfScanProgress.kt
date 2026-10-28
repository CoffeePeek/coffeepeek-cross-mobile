package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_scan_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The photo and the scanner's stages as they run, so a few seconds do not look like a hang. */
@Composable
internal fun ShelfScanProgress(photo: ByteArray, stagesDone: Set<BagScanStage>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShelfScanPhoto(photo, null, Modifier.fillMaxWidth().weight(1f))
        Text(stringResource(Res.string.shelf_scan_title), Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        ShelfScanStagesRunning(stagesDone)
    }
}

@Composable
private fun ShelfScanProgressPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanProgress(byteArrayOf(), setOf(BagScanStage.Segmentation, BagScanStage.Front),
        Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfScanProgressLightPreview() = ShelfScanProgressPreview(false)
@Preview @Composable private fun ShelfScanProgressDarkPreview() = ShelfScanProgressPreview(true)
