package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_photo_loading
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.media.ImageUrl
import com.coffeepeek.core.media.LocalImageEnvironment
import com.coffeepeek.core.media.resolveImageData
import com.coffeepeek.core.media.image.FullScreenPhotoPresence
import com.coffeepeek.core.media.image.rememberImageResource
import io.kamel.core.Resource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Preview-first image; higher quality waits for visibility and yields to an open gallery. */
@Composable
fun CoffeePeekImage(
    data: Any, placeholder: DrawableResource? = null, error: DrawableResource? = null,
    contentScale: ContentScale = ContentScale.Crop, modifier: Modifier = Modifier, contentDescription: String? = null,
    alignment: Alignment = Alignment.Center, highQualityData: Any? = null, upgradeDelayMillis: Long = 1_500L, showLoader: Boolean = false,
) {
    val base = LocalImageEnvironment.current.apiBaseUrl
    val preview = resolveImageData(data, base)
    val detail = highQualityData?.let { resolveImageData(it, base) }?.takeUnless { it == preview || it is ImageUrl && it.url.isBlank() }
    var visible by remember(preview, detail) { mutableStateOf(false) }
    val result = rememberProgressiveImageResult(preview, detail, upgradeDelayMillis, visible)
    val observed = modifier.imageVisibility(detail != null && upgradeDelayMillis > 0) { visible = it }
    CoffeePeekImageContent(result, placeholder, error, showLoader, contentScale, observed, contentDescription, alignment)
}

@Composable
private fun rememberProgressiveImageResult(preview: Any, detail: Any?, upgradeDelay: Long, visible: Boolean): Resource<Painter> {
    var ready by remember(preview, detail) { mutableStateOf(false) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val foreground = lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val mayUpgrade = detail != null && foreground && (upgradeDelay <= 0 || visible && !FullScreenPhotoPresence.active)
    LaunchedEffect(preview, detail, mayUpgrade, upgradeDelay) {
        if (mayUpgrade && !ready) {
            delay(upgradeDelay.coerceAtLeast(0))
            ready = true
        }
    }
    var detailLoaded by remember(preview, detail) { mutableStateOf(false) }
    var settled by remember(preview, detail) { mutableStateOf(false) }
    val small = rememberImageResource(preview, renewPermission = !detailLoaded)
    SideEffect { if (small is Resource.Success || small is Resource.Failure) settled = true }
    val large = if (detail != null && settled && (ready || upgradeDelay <= 0)) rememberImageResource(detail, mayUpgrade) else null
    SideEffect { detailLoaded = large is Resource.Success }
    return if (large is Resource.Success) large else small
}

@Composable
private fun CoffeePeekImageContent(
    result: Resource<Painter>, placeholder: DrawableResource?, error: DrawableResource?, loader: Boolean,
    scale: ContentScale, modifier: Modifier, description: String?, alignment: Alignment,
) {
    val loading = placeholder?.let { painterResource(it) }
        ?: ColorPainter(MaterialTheme.colorScheme.surfaceVariant.let { if (loader) it.copy(alpha = 0.3f) else it })
    val painter = when (result) {
        is Resource.Success -> result.value
        is Resource.Failure -> error?.let { painterResource(it) } ?: loading
        is Resource.Loading -> loading
    }
    if (loader) Box(contentAlignment = Alignment.Center) {
        Image(painter, description, modifier, contentScale = scale, alignment = alignment)
        if (result is Resource.Loading) CoffeePeekLoader(stringResource(Res.string.coffeepeek_photo_loading))
    } else Image(painter, description, modifier, contentScale = scale, alignment = alignment)
}

private fun Modifier.imageVisibility(enabled: Boolean, onVisible: (Boolean) -> Unit): Modifier = if (!enabled) this else
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val total = coordinates.size.width.toDouble() * coordinates.size.height
        onVisible(total > 0 && bounds.width.toDouble() * bounds.height / total >= 0.5)
    }

@Preview @Composable private fun CoffeePeekImageLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekImage(data = "", modifier = Modifier.size(120.dp), showLoader = true)
}
@Preview @Composable private fun CoffeePeekImageDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekImage(data = "", modifier = Modifier.size(120.dp), showLoader = true)
}
