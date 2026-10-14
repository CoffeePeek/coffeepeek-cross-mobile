package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException

internal fun HttpClientConfig<*>.configureBearerAuthentication(baseUrl: String, session: BearerSession) {
    val origin = Url(baseUrl.trim())
    require(origin.protocol == URLProtocol.HTTPS) { "Bearer authentication requires HTTPS" }
    fun isApi(url: Url): Boolean = url.protocol == origin.protocol &&
        url.host == origin.host && url.port == origin.port

    install(Auth) {
        // sendWithoutRequest alone would not prevent auth after an external 401 challenge.
        reAuthorizeOnResponse { it.status == HttpStatusCode.Unauthorized && isApi(it.call.request.url) }
        bearer {
            sendWithoutRequest { isApi(it.url.build()) }
            loadTokens { session.load()?.asBearerTokens() }
            refreshTokens {
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
