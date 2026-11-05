package com.coffeepeek.core.media.image

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
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
    repository: SessionReader,
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
        // Votes change counters without changing access to the check-in's photos.
        val isHelpfulVote = method in setOf("PUT", "DELETE") &&
            Regex("/api/v1/check-ins/[^/]+/helpful/?").matches(path)
        val affectsPhotos = !isHelpfulVote && method in setOf("POST", "PUT", "PATCH", "DELETE") && (
            "/check-ins" in path || "/home-check-ins" in path || "/me/blocks" in path ||
                path.trimEnd('/') == "/api/users/me"
            )
        if (!affectsPhotos) { proceed(); return@intercept }
        invalidate()
        try { proceed() } finally { invalidate() }
    }
}
