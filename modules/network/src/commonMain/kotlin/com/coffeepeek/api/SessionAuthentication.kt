package com.coffeepeek.api

import com.coffeepeek.api.model.response.AuthResp
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException

internal fun HttpClientConfig<*>.configureSessionAuthentication(
    baseUrl: String,
    current: () -> AuthResp?,
    save: (AuthResp?) -> Unit,
    refresh: suspend (String) -> AuthResp,
) {
    val origin = Url(baseUrl)
    fun isApi(url: Url) = url.protocol == origin.protocol && url.host == origin.host && url.port == origin.port
    install(Auth) {
        reAuthorizeOnResponse { it.status == HttpStatusCode.Unauthorized && isApi(it.call.request.url) }
        bearer {
            // Anonymous public endpoints never challenge, so send an available token proactively.
            sendWithoutRequest { isApi(it.url.build()) && current() != null }
            loadTokens { current()?.let { BearerTokens(it.accessToken, it.refreshToken) } }
            refreshTokens {
                val old = current() ?: return@refreshTokens null
                if (old.refreshToken.isBlank()) {
                    save(null); return@refreshTokens null
                }
                try {
                    val updated = refresh(old.refreshToken)
                    if (current()?.accessToken != old.accessToken) return@refreshTokens null
                    save(updated)
                    BearerTokens(updated.accessToken, updated.refreshToken)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (current()?.accessToken == old.accessToken) save(null)
                    null
                }
            }
        }
    }
}

internal fun HttpClient.readCurrentSessionForRequests() {
    requestPipeline.intercept(HttpRequestPipeline.State) {
        // Ktor otherwise retains the first loaded token across login, logout and account switches.
        authProvider<BearerAuthProvider>()?.clearToken()
    }
}
