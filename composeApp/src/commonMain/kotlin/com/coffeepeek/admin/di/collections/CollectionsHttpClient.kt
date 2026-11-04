package com.coffeepeek.admin.di.collections

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.admin.di.session.SessionPinnedHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder

/** Guard immediately before the engine, after bearer authentication and every authentication retry. */
internal class CollectionsHttpClient(base: HttpClient, sessions: SessionReader, verifyBearer: Boolean = true) {
    private val pinned = SessionPinnedHttpClient(base, sessions, "Collection", verifyBearer)
    val client = pinned.client

    fun pin(request: HttpRequestBuilder, owner: String) = pinned.pin(request, owner)
}
