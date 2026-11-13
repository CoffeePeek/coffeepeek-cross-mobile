package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfScannerStatus
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Keeps the chosen photo in view while preparing the scanner or offering a manual crop. */
@Composable
internal fun ShelfScanPreparation(
    photo: ByteArray, scanner: ShelfScannerStatus, onPrepare: () -> Unit, onManual: () -> Unit,
    onCamera: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.shelf_prepare_title), Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        ShelfScanPhoto(photo, null, Modifier.fillMaxWidth().height(240.dp))
        Text(stringResource(Res.string.shelf_prepare_body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ShelfPreparationStatus(scanner, onPrepare)
        ShelfSecondaryButton(stringResource(Res.string.shelf_manual_crop_action), onManual)
        ShelfSecondaryButton(stringResource(Res.string.shelf_retake_gallery), onGallery)
        ShelfSecondaryButton(stringResource(Res.string.shelf_retake_camera), onCamera)
    }
}

@Composable
private fun ShelfPreparationStatus(scanner: ShelfScannerStatus, onPrepare: () -> Unit) {
    when (scanner) {
        ShelfScannerStatus.Checking -> CoffeePeekLoader(stringResource(Res.string.shelf_scanner_checking))
        ShelfScannerStatus.Ready -> CoffeePeekLoader(stringResource(Res.string.shelf_scanner_downloading_unknown))
        is ShelfScannerStatus.NeedsDownload -> CoffeePeekButton(
            stringResource(Res.string.shelf_scanner_prepare, megabytes(scanner.bytes)), onPrepare)
        ShelfScannerStatus.DownloadFailed -> {
            Text(stringResource(Res.string.shelf_scanner_download_failed), color = MaterialTheme.colorScheme.error)
            CoffeePeekButton(stringResource(Res.string.shelf_scanner_download), onPrepare)
        }
        is ShelfScannerStatus.Downloading -> ShelfPreparationProgress(scanner)
    }
}

@Composable
private fun ShelfPreparationProgress(scanner: ShelfScannerStatus.Downloading) {
    val progress = scanner.total.takeIf { it > 0 }?.let { (scanner.done.toDouble() / it).toFloat().coerceIn(0f, 1f) }
    val label = progress?.let { stringResource(Res.string.shelf_scanner_downloading, "${(it * 100).toInt()}%") }
        ?: stringResource(Res.string.shelf_scanner_downloading_unknown)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekLoader(label, size = 20.dp, color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, progress = progress)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun megabytes(bytes: Long): Int = ((bytes + 999_999) / 1_000_000).toInt()

@Composable
private fun ShelfPreparationPreview(dark: Boolean, scanner: ShelfScannerStatus) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanPreparation(byteArrayOf(), scanner, {}, {}, {}, {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfPreparationLightPreview() = ShelfPreparationPreview(false, ShelfScannerStatus.NeedsDownload(33_000_000))
@Preview @Composable private fun ShelfPreparationDarkPreview() = ShelfPreparationPreview(true, ShelfScannerStatus.NeedsDownload(33_000_000))
@Preview @Composable private fun ShelfPreparationProgressLightPreview() = ShelfPreparationPreview(false, ShelfScannerStatus.Downloading(11_000_000, 33_000_000))
@Preview @Composable private fun ShelfPreparationProgressDarkPreview() = ShelfPreparationPreview(true, ShelfScannerStatus.Downloading(11_000_000, 33_000_000))
