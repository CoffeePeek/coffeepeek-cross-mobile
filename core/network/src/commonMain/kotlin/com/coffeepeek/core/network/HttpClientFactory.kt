package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.statement.HttpResponse

/** The caller owns the returned client and closes it at the end of its lifecycle. */
class HttpClientFactory(private val engine: HttpClientEngine) {
    fun authenticatedApi(
        baseUrl: String,
        session: BearerSession,
        diagnostics: ((NetworkDiagnostic) -> Unit)? = null,
        coordinator: SessionRefreshCoordinator? = null,
        requireHttps: Boolean = true,
        onAuthRetry: (HttpResponse) -> Unit = {},
    ): HttpClient = api(baseUrl) {
        configureBearerAuthentication(baseUrl, session, coordinator, requireHttps, onAuthRetry)
        diagnostics?.let { configureNetworkDiagnostics(it) }
    }.also { if (coordinator != null) it.readCurrentSessionForRequests() }

    fun api(
        baseUrl: String,
        configure: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient = HttpClient(engine) {
        configureApiTransport(baseUrl)
        expectSuccess = true
        configure()
    }

    fun upload(configure: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(engine) {
        configureUploadTransport()
        expectSuccess = true
        configure()
    }
}
