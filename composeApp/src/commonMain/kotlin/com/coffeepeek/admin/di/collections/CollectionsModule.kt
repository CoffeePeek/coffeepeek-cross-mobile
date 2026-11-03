package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.collections.api.CollectionsEntry
import com.coffeepeek.feature.collections.data.repository.createCollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.createCollectionsEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

internal fun collectionsModule(apiBaseUrl: String = Constants.BASE_URL) = module {
    single<CollectionsAccount> { SessionCollectionsAccount(get()) }
    single { CollectionsHttpClient(get<ApiClients>().client, get()) }
    single<CollectionsRepository> {
        val client = get<ApiClients>()
        val owned = get<CollectionsHttpClient>()
        createCollectionsRepository(owned.client, client.plainClient, apiBaseUrl, get(), owned::pin)
    }
    single<CollectionsEntry> { createCollectionsEntry(get(), get(), ::decodeCollectionPosterPhoto) }
}

internal class SessionCollectionsAccount(private val sessions: SessionReader) : CollectionsAccount {
    override val identities get() = sessions.observeSession().map { current ->
        current?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
    }.distinctUntilChanged()
    override fun currentId(): String? = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
    override suspend fun loadId(): String? { sessions.getSession(); return currentId() }
}
