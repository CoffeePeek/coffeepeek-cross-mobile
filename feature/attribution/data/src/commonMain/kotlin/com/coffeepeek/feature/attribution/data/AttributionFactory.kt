package com.coffeepeek.feature.attribution.data

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.attribution.api.AttributionFeature
import com.coffeepeek.feature.attribution.domain.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

fun createAttributionFeature(clients: ApiClients, sessions: SessionReader, directory: String, platformContext: Any?, permissionDenied: () -> Unit = {}): AttributionFeature {
    val engine = AttributionEngine(AttributionHttpBackend(clients.plainClient, sessions, clients.client), platformStore(directory),
        AttributionClock({ Clock.System.now().toEpochMilliseconds() }, ::randomKey), platformInstallChannel(platformContext), platformInstallSource(platformContext), permissionDenied)
    return DefaultAttributionFeature(engine)
}

internal class DefaultAttributionFeature(private val engine: AttributionEngine) : AttributionFeature {
    override suspend fun open(code: String): Result<String> = result { engine.open(code) }
    override suspend fun share(canonicalPath: String, placement: String): Result<String> = result { engine.share(canonicalPath, placement) }
    override suspend fun visible(canonicalPath: String) { engine.visible(canonicalPath) }
    override suspend fun sync(identity: AttributionIdentity) { engine.sync(identity) }
    override suspend fun pause() { engine.pause() }
    override suspend fun clearAccount(resetInstallation: Boolean) { engine.clearAccount(resetInstallation) }
    override suspend fun recoverHandoff(token: String, transport: String) { engine.recoverHandoff(token, transport) }
}

private suspend fun <T> result(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

internal expect fun platformStore(directory: String): AttributionStore
internal expect fun platformInstallChannel(context: Any?): String
internal expect fun platformInstallSource(context: Any?): AttributionInstallSource
