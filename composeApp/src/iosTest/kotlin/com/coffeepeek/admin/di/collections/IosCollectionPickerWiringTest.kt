package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.di.iosPlatformModules
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IosCollectionPickerWiringTest {
    @Test fun nativePlatformBindsBothHeartsToTheSameRequestOwner() = runTest {
        val requests = CollectionPickerRequests(CheckInTestSessions(), backgroundScope)
        val app = koinApplication {
            modules(iosPlatformModules("https://fixture.invalid"))
            modules(module { single { requests } })
        }
        try {
            val prompt = app.koin.get<FavoriteCollectionsPrompt>()
            prompt.shop("shop")
            assertEquals(CollectionTargetKind.CoffeeShop, assertNotNull(requests.state.value).target.kind)
            requests.dismiss(requests.state.value!!)
            prompt.roaster("roaster")
            assertEquals(CollectionTargetKind.Roaster, assertNotNull(requests.state.value).target.kind)
        } finally { app.close() }
    }
}
