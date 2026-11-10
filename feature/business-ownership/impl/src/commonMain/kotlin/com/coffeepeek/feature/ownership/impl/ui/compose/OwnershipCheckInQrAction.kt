package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.impl.resources.*
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipCheckInQrAction(shopName: String, link: String, share: (ByteArray, String) -> Unit) {
    var open by rememberSaveable(link) { mutableStateOf(false) }
    CoffeePeekOutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.ownership_checkin_qr_action))
    }
    if (open) OwnershipCheckInQrSheet(shopName, link, share) { open = false }
}

@Composable
private fun OwnershipCheckInQrSheet(shopName: String, link: String, share: (ByteArray, String) -> Unit, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val painter = remember(link) { PrintableCheckInQr(link) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    CoffeePeekSwipeDismissModalBottomSheet(onClose, stringResource(Res.string.ownership_back), dismissEnabled = !busy) {
        OwnershipCheckInQrContent(shopName, painter, busy, failed) {
            if (busy) return@OwnershipCheckInQrContent
            busy = true
            failed = false
            scope.launch {
                try {
                    val png = withContext(Dispatchers.Default) {
                        PrintableCheckInQr(link).toByteArray(1024, 1024, ImageFormat.PNG)
                    }
                    share(png, shopName)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed = true
                } finally {
                    busy = false
                }
            }
        }
    }
}

@Composable
private fun OwnershipCheckInQrContent(shopName: String, painter: Painter, busy: Boolean, failed: Boolean, onShare: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(shopName, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.ownership_checkin_qr_hint), style = MaterialTheme.typography.bodyMedium)
        Image(painter, stringResource(Res.string.ownership_checkin_qr_description, shopName),
            Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f))
        if (failed) Text(stringResource(Res.string.ownership_checkin_qr_error), color = MaterialTheme.colorScheme.error)
        CoffeePeekButton(stringResource(Res.string.ownership_checkin_qr_share), onShare, enabled = !busy)
    }
}

private class PrintableCheckInQr(link: String) : Painter() {
    private val qr = QrCodePainter(link)
    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(Color.White)
        inset(size.minDimension * 0.16f) { with(qr) { draw(size) } }
    }
}

@Composable
private fun OwnershipCheckInQrPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val link = "https://coffeepeek.by/coffee-shops/preview-shop?checkin=1"
    Column {
        OwnershipCheckInQrAction("CoffeePeek", link) { _, _ -> }
        OwnershipCheckInQrContent("CoffeePeek", remember { PrintableCheckInQr(link) }, busy = false, failed = false) {}
    }
}

@Preview @Composable private fun OwnershipCheckInQrLightPreview() = OwnershipCheckInQrPreview(false)
@Preview @Composable private fun OwnershipCheckInQrDarkPreview() = OwnershipCheckInQrPreview(true)
