package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.di.session.SessionPinnedHttpClient
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shelf.api.ShelfProfileEntry
import com.coffeepeek.feature.shelf.data.repository.createShelfProfileRepository
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.createShelfProfileEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import org.koin.dsl.module

internal class ShelfProfileHttpClient(base: HttpClient, sessions: SessionReader, verifyBearer: Boolean = true) {
    private val pinned = SessionPinnedHttpClient(base, sessions, "ShelfProfile", verifyBearer)
    val client = pinned.client
    fun pin(request: HttpRequestBuilder, owner: String) = pinned.pin(request, owner)
}

/** Separate from scanner/owner shelf construction; resolving a profile never starts model work. */
internal fun shelfProfileModule(apiBaseUrl: String) = module {
    single { ShelfProfileHttpClient(get<CoffeePeekClient>().client, get()) }
    single<ShelfProfileRepository> {
        val bound = get<ShelfProfileHttpClient>()
        createShelfProfileRepository(bound.client, get<CoffeePeekClient>().plainClient, apiBaseUrl,
            SessionShelfAccount(get()), bound::pin)
    }
    single<ShelfProfileEntry> { createShelfProfileEntry(get()) }
}
