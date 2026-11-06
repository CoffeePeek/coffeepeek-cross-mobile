package com.coffeepeek.core.media.image

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.core.media.ImageUrl
import com.coffeepeek.core.media.LocalImageEnvironment
import com.coffeepeek.core.media.resolveImageData
import com.coffeepeek.core.session.Session
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.image.asyncPainterResource
import io.kamel.image.config.LocalKamelConfig
import kotlinx.coroutines.delay

/** A decoded resource remains visible only while its owner and server permission are current. */
@Composable
fun rememberImageResource(resource: Any, renewPermission: Boolean = true): Resource<Painter> {
    val source = resolveImageData(resource, LocalImageEnvironment.current.apiBaseUrl)
    if (source is ImageUrl && source.url.isBlank()) return Resource.Loading(0f)
    if (source !is ImageUrl || !isProtectedCheckInPhoto(source.url)) return asyncPainterResource(source)
    return rememberProtectedImageResource(source, renewPermission)
}

@Composable
private fun rememberProtectedImageResource(resource: ImageUrl, renewPermission: Boolean): Resource<Painter> {
    val sessions = LocalImageEnvironment.current.sessions
    val session by sessions.observeSession().collectAsState(initial = sessions.peekSession())
    val epoch by ProtectedPhotoSession.changes.collectAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val foreground = lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val scope = ProtectedPhotoSession.scope(session)
    var generation by remember(resource, session, epoch, scope) { mutableIntStateOf(0) }
    var retries by remember(resource, session, epoch, scope, foreground) { mutableIntStateOf(0) }
    val retained = remember(resource, scope, generation, foreground) {
        if (foreground && generation == 0) protectedPhotoPainterCache.get(resource.url, scope) else null
    }
    var displayed by remember(resource, session, epoch, scope) { mutableStateOf(retained) }
    val presentation = remember(resource, session, epoch, scope, generation, foreground) {
        PhotoDisplayPermission(revalidate = generation > 0).apply { retained?.let { grant(it.permission, it.permission) } }
    }
    val permission by presentation.permission.collectAsState()
    val base = LocalKamelConfig.current
    val uncached = remember(base) { protectedKamelConfig(base) }
    val expired = rememberPhotoGrantExpiry(displayed?.permission, foreground, renewPermission) { generation++ }
    val result = protectedPainterResult(resource, session, epoch, scope, generation, presentation, foreground, retained, uncached)
    PhotoRetryEffect(result as? Resource.Failure, foreground, renewPermission, retries) {
        retries++
        generation++
    }
    return authorizedPhotoResult(resource, scope, result, presentation, permission, displayed, expired) {
        displayed = it
        if (it != null) retries = 0
    }
}

@Composable
private fun rememberPhotoGrantExpiry(grant: PhotoPermission?, foreground: Boolean, renew: Boolean, onRenew: () -> Unit): Boolean {
    var expired by remember(grant) { mutableStateOf(false) }
    LaunchedEffect(grant, foreground, renew) {
        if (grant == null) return@LaunchedEffect
        if (foreground && renew) {
            val lead = minOf(2_000L, grant.remaining(SystemPhotoCacheClock) / 2)
            while (grant.remaining(SystemPhotoCacheClock) > lead) {
                delay(minOf(grant.remaining(SystemPhotoCacheClock) - lead, 1_000).coerceAtLeast(1))
            }
            onRenew()
        }
        while (grant.remaining(SystemPhotoCacheClock) > 0) delay(minOf(grant.remaining(SystemPhotoCacheClock), 1_000).coerceAtLeast(1))
        expired = true
    }
    return expired
}

private class LoadedPhoto { var value: Resource<Painter> by mutableStateOf(Resource.Loading(0f)) }

@Composable
private fun protectedPainterResult(
    resource: ImageUrl, session: Session?, epoch: Any, scope: Any, generation: Int,
    presentation: PhotoDisplayPermission, foreground: Boolean, retained: AuthorizedPhotoPainter?, config: KamelConfig,
): Resource<Painter> {
    val loaded = remember(resource, session, epoch, scope, generation, presentation) { LoadedPhoto() }
    if (!foreground) return Resource.Loading(0f)
    if (retained != null) return Resource.Success(retained.painter, DataSource.Memory)
    CompositionLocalProvider(LocalKamelConfig provides config) {
        val result = key(resource, session, epoch, scope, generation, presentation) {
            asyncPainterResource(resource.copy(permissionScope = scope, displayPermission = presentation))
        }
        SideEffect { loaded.value = result }
    }
    return loaded.value
}

@Composable
private fun PhotoRetryEffect(failure: Resource.Failure?, foreground: Boolean, renew: Boolean, retries: Int, onRetry: () -> Unit) {
    val denied = failure?.exception is PhotoAccessDenied
    LaunchedEffect(failure, foreground, renew) {
        if (failure != null && !denied && foreground && renew && retries < 3) {
            delay(1_000L * (retries + 1))
            onRetry()
        }
    }
}

@Composable
private fun authorizedPhotoResult(
    resource: ImageUrl, scope: Any, result: Resource<Painter>, presentation: PhotoDisplayPermission,
    permission: PhotoPermission?, displayed: AuthorizedPhotoPainter?, expired: Boolean, onDisplay: (AuthorizedPhotoPainter?) -> Unit,
): Resource<Painter> {
    if (!ProtectedPhotoSession.current(scope)) return Resource.Loading(0f)
    val failure = result as? Resource.Failure
    if (failure?.exception is PhotoAccessDenied) {
        SideEffect {
            displayed?.let { protectedPhotoPainterCache.store(resource.url, scope, it.painter, null) }
            onDisplay(null)
        }
        return failure
    }
    val success = result as? Resource.Success
    val grant = permission?.takeIf { it.remaining(SystemPhotoCacheClock) > 0 }
    if (success != null && grant != null) {
        SideEffect {
            onDisplay(AuthorizedPhotoPainter(success.value, grant))
            protectedPhotoPainterCache.store(resource.url, scope, success.value, presentation.reusablePermission)
        }
        return success
    }
    if (!expired && displayed != null && displayed.permission.remaining(SystemPhotoCacheClock) > 0) {
        return Resource.Success(displayed.painter, DataSource.Memory)
    }
    return failure ?: Resource.Loading(0f)
}
