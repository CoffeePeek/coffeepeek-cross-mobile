package com.coffeepeek.admin.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
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
import com.coffeepeek.admin.utils.image.AuthorizedPhotoPainter
import com.coffeepeek.admin.utils.image.PhotoAccessDenied
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
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.photo_loading
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun CpImage(
    data: Any,
    placeholder: DrawableResource? = null,
    error: DrawableResource? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    alignment: Alignment = Alignment.Center,
    highQualityData: Any? = null,
    upgradeDelayMillis: Long = 1_500L,
    /** Draws the brand loader over a translucent tile while nothing of the image has arrived yet. */
    showLoader: Boolean = false,
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

    // The preview is loaded first and alone: the original (often a huge file) must not compete with it for the transfer and the decoder, which kept the
    // preview of the full-screen viewer spinning until the original was dropped. The original starts once the preview has settled, success or failure.
    var detailLoaded by remember(previewData, detailData) { mutableStateOf(false) }
    var previewSettled by remember(previewData, detailData) { mutableStateOf(false) }
    // Keep the selected quality through navigation; the loader gates transfers by lifecycle.
    val preview = rememberCpImageResource(previewData, renewPermission = !detailLoaded)
    SideEffect { if (preview is Resource.Success || preview is Resource.Failure) previewSettled = true }
    val detail = if (detailData != null && previewSettled && (upgradeReady || upgradeDelayMillis <= 0)) {
        rememberCpImageResource(detailData, renewPermission = mayUpgrade)
    } else null
    SideEffect { detailLoaded = detail is Resource.Success }
    // Keep the preview during the higher-quality transfer, including failures.
    // Expired protected painters are withdrawn by the resource loader itself.
    val result = if (detail is Resource.Success) detail else preview
    // With the loader the tile is only a translucent veil over the screen background, not an opaque slab.
    val loading = placeholder?.toPainterResource()
        ?: ColorPainter(MaterialTheme.colorScheme.surfaceVariant.let { if (showLoader) it.copy(alpha = 0.3f) else it })
    val painter = when (result) {
        is Resource.Success -> result.value
        is Resource.Failure -> error?.toPainterResource() ?: loading
        is Resource.Loading -> loading
    }
    val image: @Composable () -> Unit = {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            modifier = modifier.then(if (detailData != null && upgradeDelayMillis > 0) {
                Modifier.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val total = coordinates.size.width.toDouble() * coordinates.size.height
                    visible = total > 0 && bounds.width.toDouble() * bounds.height / total >= 0.5
                }
            } else Modifier),
        )
    }
    if (showLoader) Box(contentAlignment = Alignment.Center) {
        image()
        if (result is Resource.Loading) CoffeePeekLoader(stringResource(Res.string.photo_loading))
    } else image()
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

/** The latest state of one protected photo load, observable by the function that owns the load. */
private class LoadedPhoto {
    var value: Resource<Painter> by mutableStateOf(Resource.Loading(0f))
}

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
    var retries by remember(resource, session, epoch, permissionScope, foreground) { mutableIntStateOf(0) }
    val retained = remember(resource, permissionScope, generation, foreground) {
        if (foreground && generation == 0) protectedPhotoPainterCache.get(resource.url, permissionScope) else null
    }
    // A stopped navigation entry can still be visible during its exit transition.
    var displayed by remember(resource, session, epoch, permissionScope) {
        mutableStateOf(retained)
    }
    val presentation = remember(resource, session, epoch, permissionScope, generation, foreground) {
        PhotoDisplayPermission(revalidate = generation > 0).apply { retained?.let { grant(it.permission, it.permission) } }
    }
    val permission by presentation.permission.collectAsState()
    val base = LocalKamelConfig.current
    val uncached = remember(base) { protectedKamelConfig(base) }

    val displayGrant = displayed?.permission
    var expired by remember(displayGrant) { mutableStateOf(false) }
    LaunchedEffect(displayGrant, foreground, renewPermission) {
        val grant = displayGrant ?: return@LaunchedEffect
        if (foreground && renewPermission) {
            // Renew before expiry while the currently authorized painter stays visible.
            val lead = minOf(2_000L, grant.remaining(SystemPhotoCacheClock) / 2)
            while (grant.remaining(SystemPhotoCacheClock) > lead) {
                delay(minOf(grant.remaining(SystemPhotoCacheClock) - lead, 1_000).coerceAtLeast(1))
            }
            generation++
        }
        while (grant.remaining(SystemPhotoCacheClock) > 0) {
            delay(minOf(grant.remaining(SystemPhotoCacheClock), 1_000).coerceAtLeast(1))
        }
        expired = true
    }

    // The loader's state is read inside the CompositionLocalProvider lambda, which recomposes on its own: a plain local assigned there never reached this
    // function again, so a photo that finished decoding after the permission arrived stayed on Loading until something unrelated recomposed it (a touch).
    val loaded = remember(resource, session, epoch, permissionScope, generation, presentation) { LoadedPhoto() }
    var result: Resource<Painter> = Resource.Loading(0f)
    if (foreground) {
        if (retained != null) result = Resource.Success(retained.painter, DataSource.Memory)
        else {
            CompositionLocalProvider(LocalKamelConfig provides uncached) {
                val loading = key(resource, session, epoch, permissionScope, generation, presentation) {
                    asyncPainterResource(data = resource.copy(permissionScope = permissionScope, displayPermission = presentation))
                }
                SideEffect { loaded.value = loading }
            }
            result = loaded.value
        }
    }
    val failure = result as? Resource.Failure
    val denied = failure?.exception is PhotoAccessDenied
    LaunchedEffect(failure, foreground, renewPermission) {
        if (failure != null && !denied && foreground && renewPermission && retries < 3) {
            delay(1_000L * (retries + 1))
            retries++
            generation++
        }
    }
    if (!ProtectedPhotoSession.current(permissionScope)) return Resource.Loading(0f)
    if (denied) {
        SideEffect {
            displayed?.let { protectedPhotoPainterCache.store(resource.url, permissionScope, it.painter, null) }
            displayed = null
        }
        return checkNotNull(failure)
    }
    val success = result as? Resource.Success
    val grant = permission?.takeIf { it.remaining(SystemPhotoCacheClock) > 0 }
    if (success != null && grant != null) {
        val painter = success.value
        SideEffect {
            displayed = AuthorizedPhotoPainter(painter, grant)
            retries = 0
            protectedPhotoPainterCache.store(resource.url, permissionScope, painter, presentation.reusablePermission)
        }
        return success
    }
    val previous = displayed
    if (!expired && previous != null && previous.permission.remaining(SystemPhotoCacheClock) > 0) {
        return Resource.Success(previous.painter, DataSource.Memory)
    }
    return failure ?: Resource.Loading(0f)
}
