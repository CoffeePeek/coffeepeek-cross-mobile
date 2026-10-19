package com.coffeepeek.admin.utils.image

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/** Session and mutation revocation also applies to builds without a local author-blocking UI. */
internal fun installPhotoSessionInvalidation(
    repository: SessionRepository,
    client: HttpClient,
    scope: CoroutineScope,
    onSessionChanged: (Session?) -> Unit,
    invalidate: () -> Unit,
    visibility: Flow<*> = emptyFlow<Unit>(),
) {
    scope.launch { repository.observeSession().collect { onSessionChanged(it) } }
    scope.launch { visibility.drop(1).collect { invalidate() } }
    // A committed mutation may lose its response. Revoke before and after every attempt.
    client.requestPipeline.intercept(HttpRequestPipeline.State) {
        val method = context.method.value
        val path = context.url.build().encodedPath.lowercase()
        val affectsPhotos = method in setOf("POST", "PUT", "PATCH", "DELETE") && (
            "/check-ins" in path || "/home-check-ins" in path || "/me/blocks" in path ||
                path.trimEnd('/') == "/api/users/me"
            )
        if (!affectsPhotos) { proceed(); return@intercept }
        invalidate()
        try { proceed() } finally { invalidate() }
    }
}
