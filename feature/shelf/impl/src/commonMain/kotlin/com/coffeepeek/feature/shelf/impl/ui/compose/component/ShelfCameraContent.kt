package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    status: ShelfCameraStatus, torchAvailable: Boolean, torch: Boolean, focus: ShelfCameraFocus?, capturing: Boolean, frozen: ImageBitmap?,
    viewfinder: @Composable (Modifier) -> Unit, actions: ShelfCameraActions, modifier: Modifier = Modifier,
) {
    var tips by rememberSaveable { mutableStateOf(false) }
    val ready = status == ShelfCameraStatus.Ready
    Box(modifier.fillMaxSize().background(Color.Black).pointerInput(ready) {
        if (ready) detectTapGestures { actions.onFocus(it.x / size.width, it.y / size.height) }
    }) {
        if (ready || status == ShelfCameraStatus.Starting) viewfinder(Modifier.fillMaxSize())
        // The shot is being taken: hold the frame the user saw, so the press registers at once.
        if (frozen != null) Image(frozen, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        if (focus != null && frozen == null) ShelfFocusRing(focus)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ShelfCameraTopBar(actions.onClose) { tips = true }
            ShelfCameraHint(capturing, Modifier.align(Alignment.CenterHorizontally))
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp, vertical = CoffeePeekDimens.spacing6),
                contentAlignment = Alignment.Center) {
                if (status == ShelfCameraStatus.Denied || status == ShelfCameraStatus.Unavailable) ShelfCameraBlocked(status, actions.onAllow)
                else ShelfCameraGuide(Modifier.fillMaxSize())
            }
            ShelfCameraShutterBar(status == ShelfCameraStatus.Ready, capturing, torchAvailable, torch, actions)
        }
        ShelfShutterFlash(capturing)
        ShelfCameraTipsOverlay(tips) { tips = false }
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
private fun ShelfCameraHint(capturing: Boolean, modifier: Modifier) {
    Text(stringResource(if (capturing) Res.string.shelf_camera_hold else Res.string.shelf_camera_hint), modifier.clip(CircleShape).background(Scrim)
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
private fun ShelfCameraShutterBar(ready: Boolean, capturing: Boolean, torchAvailable: Boolean, torch: Boolean, actions: ShelfCameraActions) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing6, vertical = CoffeePeekDimens.spacing6),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        val gallery = stringResource(Res.string.shelf_camera_gallery)
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .background(Scrim).clickable(role = Role.Button, onClickLabel = gallery, onClick = actions.onGallery),
            contentAlignment = Alignment.Center) { Icon(CoffeePeekIcons.Gallery, gallery, tint = Color.White) }
        ShelfShutter(ready, capturing, actions.onShutter)
        ShelfTorchButton(torchAvailable, torch, actions.onTorch)
    }
}

/** A white ring around a white disc; dimmed while the camera starts or a shot is being taken. */
@Composable
private fun ShelfShutter(ready: Boolean, capturing: Boolean, onShutter: () -> Unit) {
    val label = stringResource(Res.string.shelf_camera_shutter)
    Box(Modifier.size(76.dp).alpha(if (ready) 1f else 0.4f).clip(CircleShape)
        .clickable(enabled = ready && !capturing, role = Role.Button, onClick = onShutter).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White, radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
            drawCircle(Color.White, radius = size.minDimension / 2 - (if (capturing) 14 else 9).dp.toPx())
        }
        // While the camera refocuses and shoots, the ring turns into a spinner.
        if (capturing) CircularProgressIndicator(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
    }
}

/** A round switch with a lightning bolt, the same size as the gallery button; crossed out while off. */
@Composable
private fun ShelfTorchButton(available: Boolean, torch: Boolean, onTorch: (Boolean) -> Unit) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (!available) return@Box
        val colors = MaterialTheme.colorScheme
        val label = stringResource(Res.string.shelf_camera_flash)
        val state = stringResource(if (torch) Res.string.shelf_camera_flash_on else Res.string.shelf_camera_flash_off)
        Box(Modifier.fillMaxSize().clip(CircleShape).background(if (torch) colors.primary else Scrim)
            .border(2.dp, if (torch) colors.primary else Color.White.copy(alpha = 0.7f), CircleShape)
            .toggleable(torch, role = Role.Switch, onValueChange = onTorch)
            .semantics { contentDescription = label; stateDescription = state }, contentAlignment = Alignment.Center) {
            ShelfBolt(if (torch) colors.onPrimary else Color.White, crossed = !torch)
        }
    }
}

@Composable
private fun ShelfBolt(color: Color, crossed: Boolean) = Canvas(Modifier.size(22.dp)) {
    val unit = size.width / 24f
    val bolt = Path().apply {
        moveTo(13 * unit, 2 * unit); lineTo(4 * unit, 14 * unit); lineTo(11 * unit, 14 * unit)
        lineTo(10 * unit, 22 * unit); lineTo(20 * unit, 9 * unit); lineTo(13 * unit, 9 * unit); close()
    }
    drawPath(bolt, color)
    if (crossed) drawLine(color, Offset(3 * unit, 3 * unit), Offset(21 * unit, 21 * unit), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/** A short white flash when the shutter is pressed. */
@Composable
private fun ShelfShutterFlash(capturing: Boolean) {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(capturing) {
        if (!capturing) return@LaunchedEffect
        flash.snapTo(0.85f)
        flash.animateTo(0f, tween(260))
    }
    if (flash.value > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
}

/** "Как снимать?" rises from the bottom over a dimmed viewfinder; a tap outside or "Понятно" lowers it. */
@Composable
private fun BoxScope.ShelfCameraTipsOverlay(visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible, Modifier.fillMaxSize(), enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = null, indication = null, onClick = onClose))
    }
    AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(220)) { it / 2 } + fadeOut(tween(180))) {
        ShelfCameraTips(Modifier, onClose)
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
    ShelfCameraContent(status, torchAvailable = true, torch = false, focus = ShelfCameraFocus(0.5f, 0.45f, sharp = true), capturing = false, frozen = null,
        viewfinder = { Box(it.background(Color(0xFF3A332E))) }, actions = PreviewActions)
}

@Preview @Composable private fun ShelfCameraLightPreview() = ShelfCameraPreview(false, ShelfCameraStatus.Ready)
@Preview @Composable private fun ShelfCameraDarkPreview() = ShelfCameraPreview(true, ShelfCameraStatus.Ready)
@Preview @Composable private fun ShelfCameraDeniedLightPreview() = ShelfCameraPreview(false, ShelfCameraStatus.Denied)
@Preview @Composable private fun ShelfCameraDeniedDarkPreview() = ShelfCameraPreview(true, ShelfCameraStatus.Denied)
