package com.coffeepeek.core.media

import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.core.media.image.ProtectedPhotoSession
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** Root-supplied configuration, shared by all feature entries on both platforms. */
class ImageEnvironment(val apiBaseUrl: String, val sessions: SessionReader)

val LocalImageEnvironment = staticCompositionLocalOf { ImageEnvironment("", GuestImageSession) }

/** Installs the existing session and visibility/mutation invalidation, without creating clients or credentials. */
fun installImageAccess(sessions: SessionReader, client: HttpClient, visibility: Flow<*> = emptyFlow<Unit>()) =
    ProtectedPhotoSession.install(sessions, client, visibility)

fun resolveImageData(data: Any, apiBaseUrl: String): Any = when (data) {
    is String -> ImageUrl(when {
        data.isBlank() -> ""
        data.startsWith("https://") || data.startsWith("http://") -> data
        data.startsWith("/") -> "${apiBaseUrl.trimEnd('/')}$data"
        else -> "${apiBaseUrl.trimEnd('/')}/api/file/$data"
    })
    else -> data
}

private object GuestImageSession : SessionReader {
    override fun peekSession(): Session? = null
    override fun isActiveSession(session: Session?) = false
    override fun observeSession() = flowOf<Session?>(null)
    override suspend fun getSession(): Session? = null
}
