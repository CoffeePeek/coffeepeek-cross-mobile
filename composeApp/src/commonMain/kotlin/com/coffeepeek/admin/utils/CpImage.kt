package com.coffeepeek.admin.utils

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.utils.DrawableExt.toPainterResource
import io.kamel.core.Resource
import io.kamel.image.asyncPainterResource
import io.kamel.image.config.LocalKamelConfig
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.admin.utils.image.isProtectedCheckInPhoto
import com.coffeepeek.admin.utils.image.ProtectedPhotoSession
import com.coffeepeek.admin.utils.image.PhotoDisplayPermission
import com.coffeepeek.admin.utils.image.SystemPhotoCacheClock
import com.coffeepeek.admin.utils.image.protectedKamelConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun CpImage(
    data: Any,
    placeholder: DrawableResource? = null,
    // ponytail: image_not_available.png was deleted; null → neutral color fallback in rememberCpImagePainter.
    error: DrawableResource? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = rememberCpImagePainter(cpImageData(data), placeholder, error),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
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
): androidx.compose.ui.graphics.painter.Painter {
    val loadingPlaceholder = placeholder?.toPainterResource()
        ?: ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    if (resource is CustomUrl && resource.url.isBlank()) return loadingPlaceholder
    val result = if (resource is CustomUrl && isProtectedCheckInPhoto(resource.url)) {
        val sessions = koinInject<SessionRepository>()
        val session by sessions.observeSession().collectAsState(initial = sessions.peekSession())
        val epoch by ProtectedPhotoSession.changes.collectAsState()
        val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
        val foreground = lifecycle.isAtLeast(Lifecycle.State.STARTED)
        var generation by remember(resource, session, epoch) { mutableIntStateOf(0) }
        val permissionScope = ProtectedPhotoSession.scope(session)
        val presentation = remember(resource, session, epoch, generation, foreground) { PhotoDisplayPermission() }
        val permission by presentation.permission.collectAsState()
        val base = LocalKamelConfig.current
        val uncached = remember(base) { protectedKamelConfig(base) }
        // Revoke the painter at its actual grant deadline, then reauthorize online.
        // Monotonic time prevents clock rollback extending a grant; wall time catches
        // JWT expiry/forward jumps. Background compositions release their resource.
        LaunchedEffect(presentation, permission, foreground) {
            val grant = permission ?: return@LaunchedEffect
            if (!foreground) return@LaunchedEffect
            while (grant.remaining(SystemPhotoCacheClock) > 0) {
                delay(minOf(grant.remaining(SystemPhotoCacheClock), 1_000).coerceAtLeast(1))
            }
            generation++
        }
        var current: Resource<androidx.compose.ui.graphics.painter.Painter> = Resource.Loading(0f)
        if (foreground) CompositionLocalProvider(LocalKamelConfig provides uncached) {
            current = key(resource, session, epoch, generation, presentation) {
                asyncPainterResource(data = resource.copy(permissionScope = permissionScope, displayPermission = presentation))
            }
        }
        if (permission?.remaining(SystemPhotoCacheClock)?.let { it > 0 } == true && ProtectedPhotoSession.current(permissionScope)) current
        else Resource.Loading(0f)
    } else asyncPainterResource(data = resource)
    return when (val r = result) {
        is Resource.Failure -> error?.toPainterResource() ?: loadingPlaceholder
        is Resource.Loading -> loadingPlaceholder
        is Resource.Success -> r.value
    }
}
