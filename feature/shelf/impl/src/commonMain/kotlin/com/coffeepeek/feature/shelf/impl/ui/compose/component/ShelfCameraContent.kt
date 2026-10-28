package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfCameraFocus
import com.coffeepeek.feature.shelf.api.ShelfCameraStatus
import com.coffeepeek.feature.shelf.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

// The camera chrome sits on a live picture in any theme, so it is white on translucent black rather than themed.
private val Scrim = Color.Black.copy(alpha = 0.55f)

/** Callbacks of the camera screen. */
internal class ShelfCameraActions(
    val onShutter: () -> Unit,
    val onGallery: () -> Unit,
    val onTorch: (Boolean) -> Unit,
    /** A tap on the viewfinder, as shares of its size. */
    val onFocus: (x: Float, y: Float) -> Unit,
    val onAllow: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * The bag camera: a full-screen viewfinder with a frame that sets the expectation (one bag, whole, face on), the gallery on
 * the left, the shutter in the middle and the torch on the right.
 */
@Composable
internal fun ShelfCameraContent(
    status: ShelfCameraStatus, torchAvailable: Boolean, torch: Boolean, focus: ShelfCameraFocus?, capturing: Boolean,
    viewfinder: @Composable (Modifier) -> Unit, actions: ShelfCameraActions, modifier: Modifier = Modifier,
) {
    var tips by rememberSaveable { mutableStateOf(false) }
    val ready = status == ShelfCameraStatus.Ready
    Box(modifier.fillMaxSize().background(Color.Black).pointerInput(ready) {
        if (ready) detectTapGestures { actions.onFocus(it.x / size.width, it.y / size.height) }
    }) {
        if (ready || status == ShelfCameraStatus.Starting) viewfinder(Modifier.fillMaxSize())
        if (focus != null) ShelfFocusRing(focus)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ShelfCameraTopBar(actions.onClose) { tips = true }
            ShelfCameraHint(Modifier.align(Alignment.CenterHorizontally))
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp, vertical = CoffeePeekDimens.spacing6),
                contentAlignment = Alignment.Center) {
                if (status == ShelfCameraStatus.Denied || status == ShelfCameraStatus.Unavailable) ShelfCameraBlocked(status, actions.onAllow)
                else ShelfCameraGuide(Modifier.fillMaxSize())
            }
            ShelfCameraShutterBar(status == ShelfCameraStatus.Ready && !capturing, torchAvailable, torch, actions)
        }
        if (tips) ShelfCameraTips(Modifier.align(Alignment.BottomCenter)) { tips = false }
    }
}

/** Where the user tapped: white while the lens moves, yellow once sharp, dim if it could not focus. */
@Composable
private fun ShelfFocusRing(focus: ShelfCameraFocus) {
    val color = when (focus.sharp) {
        null -> Color.White
        true -> MaterialTheme.colorScheme.primary
        false -> Color.White.copy(alpha = 0.5f)
    }
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(focus.x * size.width, focus.y * size.height)
        drawCircle(color, radius = 34.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
        drawCircle(color, radius = 3.dp.toPx(), center = center)
    }
}

@Composable
private fun ShelfCameraTopBar(onClose: () -> Unit, onTips: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClose) { Icon(CoffeePeekIcons.Close, stringResource(Res.string.shelf_camera_close), tint = Color.White) }
        TextButton(onTips) { Text(stringResource(Res.string.shelf_camera_how), color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ShelfCameraHint(modifier: Modifier) {
    Text(stringResource(Res.string.shelf_camera_hint), modifier.clip(CircleShape).background(Scrim)
        .padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing1),
        color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

/** Four rounded corners in the brand yellow, like a viewfinder mark for one upright bag. */
@Composable
private fun ShelfCameraGuide(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val arm = 26.dp.toPx()
        val radius = 10.dp.toPx()
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        listOf(Offset(0f, 0f) to Offset(1f, 1f), Offset(size.width, 0f) to Offset(-1f, 1f),
            Offset(size.width, size.height) to Offset(-1f, -1f), Offset(0f, size.height) to Offset(1f, -1f)).forEach { (corner, inward) ->
            val path = Path().apply {
                moveTo(corner.x, corner.y + inward.y * arm)
                lineTo(corner.x, corner.y + inward.y * radius)
                quadraticTo(corner.x, corner.y, corner.x + inward.x * radius, corner.y)
                lineTo(corner.x + inward.x * arm, corner.y)
            }
            drawPath(path, color, style = stroke)
        }
    }
}

@Composable
private fun ShelfCameraBlocked(status: ShelfCameraStatus, onAllow: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (status == ShelfCameraStatus.Denied) {
            Text(stringResource(Res.string.shelf_camera_denied), color = Color.White, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.shelf_camera_denied_body), color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            CoffeePeekButton(stringResource(Res.string.shelf_camera_allow), onAllow)
        } else Text(stringResource(Res.string.shelf_camera_unavailable), color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShelfCameraShutterBar(enabled: Boolean, torchAvailable: Boolean, torch: Boolean, actions: ShelfCameraActions) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing6, vertical = CoffeePeekDimens.spacing6),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        val gallery = stringResource(Res.string.shelf_camera_gallery)
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .background(Scrim).clickable(role = Role.Button, onClickLabel = gallery, onClick = actions.onGallery),
            contentAlignment = Alignment.Center) { Icon(CoffeePeekIcons.Gallery, gallery, tint = Color.White) }
        ShelfShutter(enabled, actions.onShutter)
        ShelfTorchToggle(torchAvailable, torch, actions.onTorch)
    }
}

/** A white ring around a white disc; dimmed while the camera starts or a shot is being taken. */
@Composable
private fun ShelfShutter(enabled: Boolean, onShutter: () -> Unit) {
    val label = stringResource(Res.string.shelf_camera_shutter)
    Box(Modifier.size(76.dp).alpha(if (enabled) 1f else 0.4f).clip(CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onShutter).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White, radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
            drawCircle(Color.White, radius = size.minDimension / 2 - 9.dp.toPx())
        }
    }
}

@Composable
private fun ShelfTorchToggle(available: Boolean, torch: Boolean, onTorch: (Boolean) -> Unit) {
    val text = stringResource(if (torch) Res.string.shelf_camera_flash_on else Res.string.shelf_camera_flash_off)
    Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        if (available) Text(text, Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Switch) { onTorch(!torch) }.padding(4.dp),
            color = if (torch) MaterialTheme.colorScheme.primary else Color.White, fontSize = 11.sp, textAlign = TextAlign.Center,
            lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** "Как снимать?": the three rules from the scan intro on a card over the viewfinder. */
@Composable
private fun ShelfCameraTips(modifier: Modifier, onClose: () -> Unit) {
    Surface(modifier.fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing3),
        shape = RoundedCornerShape(CoffeePeekDimens.cardRadius), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(CoffeePeekDimens.cardPadding), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Text(stringResource(Res.string.shelf_camera_how), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(Res.string.shelf_scan_tip_whole, Res.string.shelf_scan_tip_straight, Res.string.shelf_scan_tip_clean).forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                    Icon(CoffeePeekIcons.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
            CoffeePeekButton(stringResource(Res.string.shelf_camera_tips_close), onClose)
        }
    }
}

private val PreviewActions = ShelfCameraActions({}, {}, {}, { _, _ -> }, {}, {})

@Composable
private fun ShelfCameraPreview(dark: Boolean, status: ShelfCameraStatus) = CoffeePeekTheme(darkTheme = dark) {
    ShelfCameraContent(status, torchAvailable = true, torch = false, focus = ShelfCameraFocus(0.5f, 0.45f, sharp = true), capturing = false,
        viewfinder = { Box(it.background(Color(0xFF3A332E))) }, actions = PreviewActions)
}

@Preview @Composable private fun ShelfCameraLightPreview() = ShelfCameraPreview(false, ShelfCameraStatus.Ready)
@Preview @Composable private fun ShelfCameraDarkPreview() = ShelfCameraPreview(true, ShelfCameraStatus.Ready)
@Preview @Composable private fun ShelfCameraDeniedLightPreview() = ShelfCameraPreview(false, ShelfCameraStatus.Denied)
@Preview @Composable private fun ShelfCameraDeniedDarkPreview() = ShelfCameraPreview(true, ShelfCameraStatus.Denied)
