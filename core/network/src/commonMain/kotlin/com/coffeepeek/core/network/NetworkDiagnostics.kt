package com.coffeepeek.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.CancellationException

/** Deliberately contains no URL, headers, bodies, credentials or exception messages. */
data class NetworkDiagnostic(val method: Method, val outcome: Outcome, val statusCode: Int? = null) {
    enum class Method { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, OTHER }
    enum class Outcome { STARTED, RESPONSE, FAILED, CANCELLED }
}

private class DiagnosticsConfig {
    var observer: (NetworkDiagnostic) -> Unit = {}
}

private val SafeNetworkDiagnostics = createClientPlugin("SafeNetworkDiagnostics", ::DiagnosticsConfig) {
    val observer = pluginConfig.observer
    fun emit(event: NetworkDiagnostic) {
        try {
            observer(event)
        } catch (_: Exception) {
            // A diagnostic sink failure must not alter request behaviour.
        }
    }
    on(Send) { request ->
        val method = NetworkDiagnostic.Method.entries.firstOrNull { it.name == request.method.value }
            ?: NetworkDiagnostic.Method.OTHER
        emit(NetworkDiagnostic(method, NetworkDiagnostic.Outcome.STARTED))
        try {
            proceed(request).also {
                emit(NetworkDiagnostic(method, NetworkDiagnostic.Outcome.RESPONSE, it.response.status.value))
            }
        } catch (cancelled: CancellationException) {
            emit(NetworkDiagnostic(method, NetworkDiagnostic.Outcome.CANCELLED))
            throw cancelled
        } catch (failure: Exception) {
            emit(NetworkDiagnostic(method, NetworkDiagnostic.Outcome.FAILED))
            throw failure
        }
    }
}

/** No observer is installed by default; composition chooses whether/where to record events. */
fun HttpClientConfig<*>.configureNetworkDiagnostics(observer: (NetworkDiagnostic) -> Unit) {
    install(SafeNetworkDiagnostics) { this.observer = observer }
}
