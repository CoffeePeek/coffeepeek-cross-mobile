package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.collections.api.CollectionsEntry
import com.coffeepeek.feature.collections.data.repository.createCollectionsRepository
import com.coffeepeek.feature.collections.data.repository.createSavedCollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.createCollectionsEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.dsl.module
import org.koin.core.module.Module

internal fun collectionsModule(apiBaseUrl: String = Constants.BASE_URL, trackPickerFavorites: Boolean = false) = module {
    single<CollectionsAccount> { SessionCollectionsAccount(get()) }
    single { CollectionsHttpClient(get<ApiClients>().client, get()) }
    collectionsRepositories(apiBaseUrl, trackPickerFavorites)
    single<CollectionsEntry> { createCollectionsEntry(get(), get(), get(), ::decodeCollectionPosterPhoto, collectionCoverPicker) }
}

private fun Module.collectionsRepositories(apiBaseUrl: String, trackPickerFavorites: Boolean) {
    single<CollectionsRepository> {
        val client = get<ApiClients>()
        val owned = get<CollectionsHttpClient>()
        createCollectionsRepository(owned.client, client.plainClient, apiBaseUrl, get(), owned::pin)
    }
    single<SavedCollectionsRepository> {
        val client = get<ApiClients>()
        val owned = get<CollectionsHttpClient>()
        val repository = createSavedCollectionsRepository(owned.client, client.plainClient, apiBaseUrl, get(), owned::pin)
        if (trackPickerFavorites) FavoriteMembershipAnalyticsRepository(repository, get(), get()) else repository
    }
}

internal class SessionCollectionsAccount(private val sessions: SessionReader) : CollectionsAccount {
    override val identities get() = sessions.observeSession().map { current ->
        current?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
    }.distinctUntilChanged()
    override fun currentId(): String? = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
    override suspend fun loadId(): String? { sessions.getSession(); return currentId() }
}
