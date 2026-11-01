package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfScannerStatus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** How to photograph a bag, and the camera and gallery once the scanner's models are on the phone. */
@Composable
internal fun ShelfScanIntro(scanner: ShelfScannerStatus, onCamera: () -> Unit, onGallery: () -> Unit, onDownload: () -> Unit,
    modifier: Modifier = Modifier) {
    val ready = scanner == ShelfScannerStatus.Ready
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.shelf_scan_intro_title), Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.shelf_scan_intro_body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(Res.string.shelf_scan_tip_whole, Res.string.shelf_scan_tip_straight, Res.string.shelf_scan_tip_clean).forEach {
            ShelfScanTip(stringResource(it))
        }
        ShelfScannerReadiness(scanner, onDownload)
        CoffeePeekButton(stringResource(Res.string.shelf_scan_camera), onCamera, enabled = ready,
            leadingIcon = { Icon(CoffeePeekIcons.Camera, null) })
        ShelfSecondaryButton(stringResource(Res.string.shelf_scan_gallery), onGallery, enabled = ready)
    }
}

@Composable
private fun ShelfScanTip(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
        Icon(CoffeePeekIcons.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Nothing once ready; otherwise the one-time model download and its progress. */
@Composable
private fun ShelfScannerReadiness(scanner: ShelfScannerStatus, onDownload: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (scanner) {
        ShelfScannerStatus.Ready -> Unit
        ShelfScannerStatus.Checking -> CoffeePeekLoader(stringResource(Res.string.shelf_scanner_checking))
        is ShelfScannerStatus.NeedsDownload -> Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Text(stringResource(Res.string.shelf_scanner_download_body, megabytes(scanner.bytes)), color = muted)
            CoffeePeekButton(stringResource(Res.string.shelf_scanner_download), onDownload)
        }
        is ShelfScannerStatus.Downloading -> ShelfScannerDownloadProgress(scanner)
        ShelfScannerStatus.DownloadFailed -> Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Text(stringResource(Res.string.shelf_scanner_download_failed), color = MaterialTheme.colorScheme.error)
            CoffeePeekButton(stringResource(Res.string.shelf_scanner_download), onDownload)
        }
    }
}

@Composable
private fun ShelfScannerDownloadProgress(scanner: ShelfScannerStatus.Downloading) {
    val share = scanner.total.takeIf { it > 0 }?.let { (scanner.done.toDouble() / it).toFloat().coerceIn(0f, 1f) }
    val label = share?.let { stringResource(Res.string.shelf_scanner_downloading, "${(it * 100).toInt()}%") }
        ?: stringResource(Res.string.shelf_scanner_downloading_unknown)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekLoader(label, size = 20.dp, color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, progress = share)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun megabytes(bytes: Long): Int = ((bytes + 999_999) / 1_000_000).toInt()

@Composable
private fun ShelfScanIntroPreview(dark: Boolean, scanner: ShelfScannerStatus) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanIntro(scanner, {}, {}, {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfScanIntroLightPreview() = ShelfScanIntroPreview(false, ShelfScannerStatus.Ready)
@Preview @Composable private fun ShelfScanIntroDarkPreview() = ShelfScanIntroPreview(true, ShelfScannerStatus.Ready)
@Preview @Composable private fun ShelfScanIntroDownloadLightPreview() = ShelfScanIntroPreview(false, ShelfScannerStatus.NeedsDownload(28_079_181))
@Preview @Composable private fun ShelfScanIntroDownloadDarkPreview() = ShelfScanIntroPreview(true, ShelfScannerStatus.NeedsDownload(28_079_181))
@Preview @Composable private fun ShelfScanIntroDownloadingLightPreview() = ShelfScanIntroPreview(false, ShelfScannerStatus.Downloading(9_000_000, 28_079_181))
@Preview @Composable private fun ShelfScanIntroDownloadingDarkPreview() = ShelfScanIntroPreview(true, ShelfScannerStatus.Downloading(9_000_000, 28_079_181))
@Preview @Composable private fun ShelfScanIntroUnknownDownloadLightPreview() = ShelfScanIntroPreview(false, ShelfScannerStatus.Downloading(9_000_000, 0))
@Preview @Composable private fun ShelfScanIntroUnknownDownloadDarkPreview() = ShelfScanIntroPreview(true, ShelfScannerStatus.Downloading(9_000_000, 0))
