package com.coffeepeek.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.content.OutgoingContent
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase

private object AnonymousCacheRequests : HttpClientPlugin<Unit, AnonymousCacheRequests> {
    override val key = AttributeKey<AnonymousCacheRequests>("AnonymousCacheRequests")

    override fun prepare(block: Unit.() -> Unit): AnonymousCacheRequests = this

    override fun install(plugin: AnonymousCacheRequests, scope: HttpClient) {
        val phase = PipelinePhase("AnonymousCacheRequests")
        scope.sendPipeline.insertPhaseAfter(HttpSendPipeline.State, phase)
        scope.sendPipeline.intercept(phase) { content ->
            val contentHeaders = (content as? OutgoingContent)?.headers
            val hasCredentials = context.headers.names().hasSessionCredentials() ||
                contentHeaders?.names()?.hasSessionCredentials() == true
            require(!hasCredentials) {
                "Public cache client must not send session credentials"
            }
        }
    }
}

private fun Set<String>.hasSessionCredentials(): Boolean =
    any { it.equals(HttpHeaders.Authorization, ignoreCase = true) || it.equals(HttpHeaders.Cookie, ignoreCase = true) }

/** Opt-in anonymous HTTP cache; storage capacity, persistence and cleanup belong to the caller. */
fun HttpClientConfig<*>.configurePublicHttpCache(storage: CacheStorage) {
    install(AnonymousCacheRequests)
    install(HttpCache) {
        isShared = true
        publicStorage(AnonymousCacheStorage(storage))
        privateStorage(CacheStorage.Disabled)
    }
}

private class AnonymousCacheStorage(private val delegate: CacheStorage) : CacheStorage by delegate {
    private fun CachedResponseData.isAnonymous(): Boolean = !headers.contains(HttpHeaders.SetCookie) &&
        varyKeys.keys.none { it.equals(HttpHeaders.Authorization, true) || it.equals(HttpHeaders.Cookie, true) }

    override suspend fun store(url: Url, data: CachedResponseData) {
        if (data.isAnonymous()) delegate.store(url, data)
    }

    override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? =
        delegate.find(url, varyKeys)?.takeIf { it.isAnonymous() }

    override suspend fun findAll(url: Url): Set<CachedResponseData> =
        delegate.findAll(url).filter { it.isAnonymous() }.toSet()
}
