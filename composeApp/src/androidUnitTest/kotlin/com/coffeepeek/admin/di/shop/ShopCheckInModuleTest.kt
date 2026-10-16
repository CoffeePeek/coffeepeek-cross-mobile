package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ShopCheckInModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun resolvesOneGraphWithoutOpeningDraftOrReadingCredentials() {
        var credentialReads = 0
        val client = CoffeePeekClient("https://fixture.invalid", temporary.newFolder("cache").path, false,
            getToken = { credentialReads++; null }, saveToken = { error("Unexpected credentials write") })
        val drafts = CheckInDraftStore { 1791547200000 }
        val existing = drafts.open("internal-id").copy(note = "Retained draft")
        drafts.save(existing)
        val app = koinApplication {
            modules(module {
                single { client }
                single { drafts }
                single<SessionRepository> { CheckInTestSessions() }
            }, shopCheckInModule(), shopBrowsingModule(fileBaseUrl = "https://fixture.invalid"))
        }
        try {
            assertSame(app.koin.get<ShopCheckInCreateEntry>(), app.koin.get<ShopCheckInCreateEntry>())
            assertEquals(1, app.koin.getAll<ShopCheckInRepository>().size)
            assertEquals(1, app.koin.getAll<ShopDetailsRepository>().size)
            assertEquals(1, app.koin.getAll<ShopCheckInVoteRepository>().size)
            assertSame(app.koin.get<AndroidShopCheckInDraftStore>(), app.koin.get<AndroidShopCheckInDraftStore>())
            assertNotNull(app.koin.get<ShopCheckInDateFormatter>())
            assertNotNull(app.koin.get<ShopCheckInPhotoPicker>())
            assertSame(app.koin.get<ShopDrinkPhotoProcessor>(), app.koin.get<ShopDrinkPhotoProcessor>())
            assertSame(existing, drafts.peek())
            assertEquals(0, credentialReads)
        } finally {
            app.close()
            client.client.close()
            client.plainClient.close()
            client.uploadClient.close()
        }
    }
}
