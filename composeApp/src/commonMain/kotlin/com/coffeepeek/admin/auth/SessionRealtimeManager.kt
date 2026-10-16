package com.coffeepeek.admin.auth

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.local_data_cleanup_error
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString

private const val DEFAULT_REALTIME_SESSION_URL = "https://api.coffeepeek.by/realtime/session"
private const val FORCE_LOGOUT_METHOD = "ForceLogout"
private const val RECORD_SEPARATOR = '\u001e'
private const val RECONNECT_DELAY_MILLIS = 5_000L

/**
 * Minimal SignalR JSON client used by both Android and iOS for session revocation events.
 * The server-facing protocol is deliberately limited to negotiate, handshake, keepalive,
 * close and the ForceLogout invocation that the application consumes.
 */
class SessionRealtimeManager(
    private val sessionRepository: SessionRepository,
    private val userSessionCleaner: UserSessionCleaner,
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectionMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private var connectionJob: Job? = null
    private var activeAccessToken: String? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        sessionRepository.observeSession()
            .map(::activeAccessTokenOrNull)
            .distinctUntilChanged()
            .onEach(::replaceConnection)
            .launchIn(scope)
    }

    private fun activeAccessTokenOrNull(session: Session?): String? =
        session?.accessToken?.takeIf { sessionRepository.isActiveSession(session) && it.isNotBlank() }

    private suspend fun replaceConnection(accessToken: String?) {
        connectionMutex.withLock {
            if (activeAccessToken == accessToken && connectionJob?.isActive == true) return
            connectionJob?.cancelAndJoin()
            connectionJob = null
            activeAccessToken = accessToken
            if (accessToken != null) {
                connectionJob = scope.launch { connectWithRetry(accessToken) }
            }
        }
    }

    private suspend fun connectWithRetry(accessToken: String) {
        while (scope.isActive && activeAccessToken == accessToken) {
            runCatching { connectOnce(accessToken) }
            if (scope.isActive && activeAccessToken == accessToken) {
                delay(RECONNECT_DELAY_MILLIS)
            }
        }
    }

    private suspend fun connectOnce(accessToken: String) {
        val hubUrl = resolveRealtimeSessionUrl(baseUrl)
        val negotiate = httpClient.post("$hubUrl/negotiate") {
            parameter("negotiateVersion", 1)
            bearerAuth(accessToken)
        }
        if (!negotiate.status.isSuccess()) {
            error("SignalR negotiate failed: ${negotiate.status.value}")
        }
        val payload = json.decodeFromString<NegotiateResponse>(negotiate.body())
        val connectionToken = payload.connectionToken ?: payload.connectionId
            ?: error("SignalR negotiate response has no connection token")
        val socketUrl = hubUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
            "?id=${connectionToken.encodeURLParameter()}"

        httpClient.webSocket(
            urlString = socketUrl,
            request = { headers.append(HttpHeaders.Authorization, "Bearer $accessToken") },
        ) {
            send(Frame.Text("{\"protocol\":\"json\",\"version\":1}$RECORD_SEPARATOR"))
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                frame.readText()
                    .split(RECORD_SEPARATOR)
                    .filter(String::isNotBlank)
                    .forEach { message -> handleMessage(message) }
            }
        }
    }

    private suspend fun handleMessage(raw: String) {
        val message = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return
        when (message["type"]?.jsonPrimitive?.intOrNull) {
            1 -> handleInvocation(message)
            7 -> error(message["error"]?.jsonPrimitive?.contentOrNull ?: "SignalR connection closed")
        }
    }

    private suspend fun handleInvocation(message: JsonObject) {
        val target = message["target"]?.jsonPrimitive?.contentOrNull ?: return
        if (!target.equals(FORCE_LOGOUT_METHOD, ignoreCase = true)) return
        val payload = message["arguments"]
            ?.jsonArray
            ?.firstOrNull()
            ?.let { runCatching { json.decodeFromJsonElement<ForceLogoutPayload>(it) }.getOrNull() }
        val reason = payload?.reason?.trim()
        activeAccessToken = null
        val cleanupFailure = try {
            userSessionCleaner.clearLocalUserData()
            null
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            error
        }
        if (reason == "user_deleted") {
            GoogleAuth.signOut()
            Navigator.openLoginAfterSessionEnd()
        }
        val message = forceLogoutMessage(reason)
        ErrorHandler.showError(
            if (cleanupFailure == null) message
            else "$message ${getString(Res.string.local_data_cleanup_error)}.",
        )
    }

    fun close() {
        started = false
        activeAccessToken = null
        scope.cancel()
    }
}

private fun forceLogoutMessage(reason: String?): String = when (reason?.trim()) {
    "session_revoked" -> "Текущая сессия отозвана. Войдите снова."
    "all_sessions_revoked" -> "Все сессии отозваны. Войдите снова."
    "user_blocked" -> "Аккаунт заблокирован."
    "user_deleted" -> "Аккаунт удалён."
    "password_changed" -> "Пароль изменён. Войдите снова."
    "password_reset" -> "Пароль сброшен. Войдите снова."
    else -> "Сессия завершена на сервере. Войдите снова."
}

private fun resolveRealtimeSessionUrl(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/')
    if (normalized.isBlank()) return DEFAULT_REALTIME_SESSION_URL
    return "$normalized/realtime/session"
}

@Serializable
private data class NegotiateResponse(
    val connectionToken: String? = null,
    val connectionId: String? = null,
)

@Serializable
private data class ForceLogoutPayload(
    val reason: String? = null,
    val occurredAtUtc: String? = null,
)
