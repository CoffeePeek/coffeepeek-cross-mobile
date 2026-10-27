package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException

/**
 * With a [coordinator] the clients of one session share one rotation (see [SessionRefreshCoordinator]) and the refresh
 * keeps its safeguards: it answers only the rejection of the credential that is still current, and a rotation that
 * another client already saved is reused. [onAuthRetry] is told about every rejection that starts a refresh and retry.
 */
internal fun HttpClientConfig<*>.configureBearerAuthentication(
    baseUrl: String,
    session: BearerSession,
    coordinator: SessionRefreshCoordinator? = null,
    requireHttps: Boolean = true,
    onAuthRetry: (HttpResponse) -> Unit = {},
) {
    val origin = Url(baseUrl.trim())
    if (requireHttps) require(origin.protocol == URLProtocol.HTTPS) { "Bearer authentication requires HTTPS" }
    fun isApi(url: Url): Boolean = url.protocol == origin.protocol &&
        url.host == origin.host && url.port == origin.port

    install(Auth) {
        // sendWithoutRequest alone would not prevent auth after an external 401 challenge.
        reAuthorizeOnResponse {
            (it.status == HttpStatusCode.Unauthorized && isApi(it.call.request.url)).also { retry -> if (retry) onAuthRetry(it) }
        }
        bearer {
            sendWithoutRequest { isApi(it.url.build()) }
            loadTokens { session.load()?.asBearerTokens() }
            refreshTokens {
                if (coordinator != null) return@refreshTokens refreshShared(session, coordinator, response, onAuthRetry)
                val current = session.load() ?: return@refreshTokens null
                val refresh = current.refreshToken?.takeIf { it.isNotBlank() }
                if (refresh == null) {
                    session.replace(current, null)
                    return@refreshTokens null
                }
                val updated = try {
                    session.refresh(refresh).getOrThrow()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: RefreshRejectedException) {
                    session.replace(current, null)
                    return@refreshTokens null
                }
                if (!session.replace(current, updated)) return@refreshTokens null
                updated.asBearerTokens()
            }
        }
    }
}

/** Call after external login/logout updates; Ktor caches loaded credentials per client. */
fun HttpClient.clearBearerTokenCache() {
    authProvider<BearerAuthProvider>()?.clearToken()
}

private fun SessionTokens.asBearerTokens() = BearerTokens(accessToken, refreshToken)

private suspend fun refreshShared(
    session: BearerSession,
    coordinator: SessionRefreshCoordinator,
    response: HttpResponse,
    onAuthRetry: (HttpResponse) -> Unit,
): BearerTokens? {
    onAuthRetry(response)
    val rejected = response.call.request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)
        ?: return null
    val old = session.load() ?: return null
    val refresh = old.refreshToken?.takeIf { it.isNotBlank() }
    if (refresh == null) {
        if (old.accessToken == rejected) session.replace(old, null)
        return null
    }
    return try {
        val updated = coordinator.refresh(refresh, rejected)
        val latest = session.load()
        if (latest?.accessToken != old.accessToken) {
            // A shared coordinator may already have persisted this exact rotation. Never substitute an unrelated login/account.
            if (latest != updated) return null
        } else if (!session.replace(old, updated)) return null
        updated.asBearerTokens()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: RefreshRejectedException) {
        session.replace(old, null)
        null
    }
}

/** Ktor otherwise retains the first loaded token across login, logout and account switches: read the session on every request. */
fun HttpClient.readCurrentSessionForRequests() {
    requestPipeline.intercept(HttpRequestPipeline.State) {
        authProvider<BearerAuthProvider>()?.clearToken()
    }
}
