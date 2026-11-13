package com.coffeepeek.admin.di.shop

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ShopCheckInModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun resolvesOneGraphWithoutOpeningDraftOrReadingCredentials() {
        var credentialReads = 0
        val client = ApiClients("https://fixture.invalid", temporary.newFolder("cache").path, false,
            credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { credentialReads++; return null }
                override suspend fun save(tokens: SessionTokens?) { error("Unexpected credentials write") }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } })
        val draftFolder = temporary.root.resolve("drafts")
        val app = koinApplication {
            modules(com.coffeepeek.admin.di.community.communityWritesTestModule(), module {
                single { client }
                single<SessionStore> { CheckInTestSessions() }
                single<SessionReader> { get<SessionStore>() }
            }, checkInHistoryModule("https://fixture.invalid"),
                shopCheckInModule(AndroidShopCheckInDateFormatter(), draftPath = { draftFolder.path }),
                shopBrowsingModule(fileBaseUrl = "https://fixture.invalid"))
        }
        try {
            assertSame(app.koin.get<ShopCheckInCreateEntry>(), app.koin.get<ShopCheckInCreateEntry>())
            assertEquals(1, app.koin.getAll<ShopCheckInRepository>().size)
            assertEquals(1, app.koin.getAll<ShopDetailsRepository>().size)
            assertEquals(1, app.koin.getAll<ShopCheckInVoteRepository>().size)
            assertSame(app.koin.get<AppShopCheckInDraftStore>(), app.koin.get<AppShopCheckInDraftStore>())
            assertNotNull(app.koin.get<ShopCheckInDateFormatter>())
            assertNotNull(app.koin.get<ShopCheckInPhotoPicker>())
            assertFalse(draftFolder.exists())
            assertEquals(0, credentialReads)
        } finally {
            app.close()
            client.client.close()
            client.plainClient.close()
            client.uploadClient.close()
        }
    }
}
