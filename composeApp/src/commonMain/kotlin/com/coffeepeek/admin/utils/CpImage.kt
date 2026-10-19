package com.coffeepeek.admin.utils

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.utils.DrawableExt.toPainterResource
import com.coffeepeek.admin.utils.image.PhotoDisplayPermission
import com.coffeepeek.admin.utils.image.FullScreenPhotoPresence
import com.coffeepeek.admin.utils.image.ProtectedPhotoSession
import com.coffeepeek.admin.utils.image.SystemPhotoCacheClock
import com.coffeepeek.admin.utils.image.isImmutablePhotoVariant
import com.coffeepeek.admin.utils.image.isProtectedCheckInPhoto
import com.coffeepeek.admin.utils.image.protectedKamelConfig
import com.coffeepeek.admin.utils.image.protectedPhotoPainterCache
import com.coffeepeek.domain.repository.SessionRepository
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.image.asyncPainterResource
import io.kamel.image.config.LocalKamelConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.koinInject

@Composable
fun CpImage(
    data: Any,
    placeholder: DrawableResource? = null,
    error: DrawableResource? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    highQualityData: Any? = null,
    upgradeDelayMillis: Long = 1_500L,
) {
    val previewData = cpImageData(data)
    val detailData = highQualityData?.let(::cpImageData)?.takeUnless {
        it == previewData || (it is CustomUrl && it.url.isBlank())
    }
    var visible by remember(previewData, detailData) { mutableStateOf(false) }
    var upgradeReady by remember(previewData, detailData) { mutableStateOf(false) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val foreground = lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val mayUpgrade = detailData != null && foreground &&
        (upgradeDelayMillis <= 0 || visible && !FullScreenPhotoPresence.active)
    LaunchedEffect(previewData, detailData, mayUpgrade, upgradeDelayMillis) {
        if (mayUpgrade && !upgradeReady) {
            delay(upgradeDelayMillis.coerceAtLeast(0))
            upgradeReady = true
        }
    }

    val detail = if (mayUpgrade && (upgradeReady || upgradeDelayMillis <= 0)) {
        rememberCpImageResource(checkNotNull(detailData))
    } else null
    val preview = rememberCpImageResource(previewData, renewPermission = detail !is Resource.Success)
    // Keep the preview during the higher-quality transfer, including failures.
    // Expired protected painters are withdrawn by the resource loader itself.
    val result = if (detail is Resource.Success) detail else preview
    val loading = placeholder?.toPainterResource() ?: ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val painter = when (result) {
        is Resource.Success -> result.value
        is Resource.Failure -> error?.toPainterResource() ?: loading
        is Resource.Loading -> loading
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.then(if (detailData != null && upgradeDelayMillis > 0) {
            Modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val total = coordinates.size.width.toDouble() * coordinates.size.height
                visible = total > 0 && bounds.width.toDouble() * bounds.height / total >= 0.5
            }
        } else Modifier),
    )
}

/** Bounded, one-shot warming through the same renderer/cache as visible images. */
@Composable
internal fun PrefetchPhotos(urls: List<String>, inPhotoViewer: Boolean = false) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (!lifecycle.isAtLeast(Lifecycle.State.STARTED) || FullScreenPhotoPresence.active && !inPhotoViewer) return
    var started by remember(urls) { mutableStateOf(false) }
    // Visible rows claim transfer slots first; fast scrolls cancel speculative work.
    LaunchedEffect(urls) { delay(150); started = true }
    if (!started) return
    val candidates = urls.asSequence().filter(String::isNotBlank).distinct()
        .filter { !isProtectedCheckInPhoto(it) || isImmutablePhotoVariant(it) }
        .take(6).toList()
    var completed by remember(candidates) { mutableStateOf(emptySet<String>()) }
    // A global limit also covers simultaneous list/gallery prefetch windows.
    candidates.filterNot { it in completed }.take(2).forEach { url ->
        key(url) {
            var acquired by remember { mutableStateOf(false) }
            val finished = remember { CompletableDeferred<Unit>() }
            LaunchedEffect(url) {
                photoPrefetchRequests.withPermit {
                    acquired = true
                    finished.await()
                }
            }
            if (acquired) {
                val result = rememberCpImageResource(cpImageData(url), renewPermission = false)
                if (result is Resource.Success || result is Resource.Failure) {
                    SideEffect { completed = completed + url; finished.complete(Unit) }
                }
            }
        }
    }
}

private val photoPrefetchRequests = Semaphore(2)

internal fun cpImageData(data: Any): Any = when (data) {
    is String -> CustomUrl(when {
        data.isBlank() -> ""
        data.startsWith("https://") || data.startsWith("http://") -> data
        data.startsWith("/") -> "${Constants.BASE_URL.trimEnd('/')}$data"
        else -> "${Constants.BASE_URL}api/file/$data"
    })
    else -> data
}

@Composable
fun rememberCpImagePainter(
    resource: Any,
    placeholder: DrawableResource? = null,
    error: DrawableResource? = null,
): Painter {
    val loading = placeholder?.toPainterResource() ?: ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    return when (val result = rememberCpImageResource(resource)) {
        is Resource.Success -> result.value
        is Resource.Failure -> error?.toPainterResource() ?: loading
        is Resource.Loading -> loading
    }
}

@Composable
internal fun rememberCpImageResource(resource: Any, renewPermission: Boolean = true): Resource<Painter> {
    if (resource is CustomUrl && resource.url.isBlank()) return Resource.Loading(0f)
    if (resource !is CustomUrl || !isProtectedCheckInPhoto(resource.url)) {
        return asyncPainterResource(data = resource)
    }

    val sessions = koinInject<SessionRepository>()
    val session by sessions.observeSession().collectAsState(initial = sessions.peekSession())
    val epoch by ProtectedPhotoSession.changes.collectAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val foreground = lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val permissionScope = ProtectedPhotoSession.scope(session)
    var generation by remember(resource, session, epoch, permissionScope) { mutableIntStateOf(0) }
    val retained = remember(resource, permissionScope, generation, foreground) {
        if (foreground) protectedPhotoPainterCache.get(resource.url, permissionScope) else null
    }
    val presentation = remember(resource, session, epoch, permissionScope, generation, foreground) {
        PhotoDisplayPermission().apply { retained?.let { grant(it.permission, it.permission) } }
    }
    val permission by presentation.permission.collectAsState()
    val base = LocalKamelConfig.current
    val uncached = remember(base) { protectedKamelConfig(base) }

    // Prefetch gets one permission only: it must not poll/revalidate unseen images.
    LaunchedEffect(presentation, permission, foreground, renewPermission) {
        val grant = permission ?: return@LaunchedEffect
        if (!foreground || !renewPermission) return@LaunchedEffect
        while (grant.remaining(SystemPhotoCacheClock) > 0) {
            delay(minOf(grant.remaining(SystemPhotoCacheClock), 1_000).coerceAtLeast(1))
        }
        generation++
    }

    var result: Resource<Painter> = Resource.Loading(0f)
    if (foreground) {
        if (retained != null) result = Resource.Success(retained.painter, DataSource.Memory)
        else CompositionLocalProvider(LocalKamelConfig provides uncached) {
            result = key(resource, session, epoch, permissionScope, generation, presentation) {
                asyncPainterResource(data = resource.copy(permissionScope = permissionScope, displayPermission = presentation))
            }
        }
    }
    val authorized = permission?.remaining(SystemPhotoCacheClock)?.let { it > 0 } == true &&
        ProtectedPhotoSession.current(permissionScope)
    if (!authorized) return if (result is Resource.Failure) result else Resource.Loading(0f)
    val success = result as? Resource.Success
    if (success != null) {
        val painter = success.value
        SideEffect {
            protectedPhotoPainterCache.store(resource.url, permissionScope, painter, presentation.reusablePermission)
        }
    }
    return result
}
