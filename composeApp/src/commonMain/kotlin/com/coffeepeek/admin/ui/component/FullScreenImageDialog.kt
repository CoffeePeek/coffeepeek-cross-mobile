package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) = FullScreenImageDialog(imageUrls = listOf(imageUrl), initialIndex = 0, onDismiss = onDismiss)

/** Full-screen photo viewer; swipe horizontally between [imageUrls], tap anywhere to close. */
@Composable
fun FullScreenImageDialog(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (imageUrls.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex),
        pageCount = { imageUrls.size },
    )
    // While the current photo is zoomed, drags pan the photo instead of switching pages.
    var isZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !isZoomed,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val isCurrent = pagerState.currentPage == page
                ZoomableImage(
                    imageUrl = imageUrls[page],
                    contentDescription = "Фото ${page + 1} из ${imageUrls.size}",
                    isCurrent = isCurrent,
                    onTap = onDismiss,
                    onZoomChanged = { if (isCurrent) isZoomed = it },
                )
            }

            if (imageUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = CpDimens.spacing4)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing1),
                )
            }

            PlatformGlassIconButton(
                icon = GlassControlIcon.Close,
                onClick = onDismiss,
                contentDescription = "Закрыть",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(CpDimens.spacing3),
            ) {
                Icon(
                    imageVector = CpIcons.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Pinch to zoom (1–5×), drag to pan while zoomed, double-tap to zoom in/out at the tap point. */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    contentDescription: String,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    fun clamp(value: Offset, forScale: Float): Offset {
        val maxX = size.width * (forScale - 1f) / 2f
        val maxY = size.height * (forScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    // Swiped away → reset, so the photo is unzoomed when the user comes back to it.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    LaunchedEffect(scale > 1f) { onZoomChanged(scale > 1f) }

    CoffeeShopImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        val startScale = scale
                        val startOffset = offset
                        val targetScale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val targetOffset = clamp((center - tap) * (targetScale - 1f), targetScale)
                        scope.launch {
                            animate(0f, 1f, animationSpec = tween(250)) { t, _ ->
                                scale = startScale + (targetScale - startScale) * t
                                offset = startOffset + (targetOffset - startOffset) * t
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.count { it.pressed } > 1
                        // One finger at 1× is left to the pager (page swipe) and tap detector.
                        if (pinching || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            // Zoom around the fingers' centroid, then apply the pan.
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val anchor = if (centroid.isSpecified) centroid - center else Offset.Zero
                            val scaled = (offset - anchor) * (newScale / scale) + anchor
                            offset = clamp(scaled + event.calculatePan(), newScale)
                            scale = newScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale <= 1f) offset = Offset.Zero
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}
