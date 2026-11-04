package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CollectionPromptRoasterFavoritesTest {
    private val roaster = CatalogItem("local", "Roaster", address = PublicAddress("canonical", "/roasters/canonical", 1, false))
    private class Writer : RoasterFavorites {
        var result: Result<Unit> = Result.success(Unit)
        var afterWrite: () -> Unit = {}
        val writes = mutableListOf<Boolean>()
        override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
        override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
            writes += isFavorite
            afterWrite()
            return result
        }
    }

    @Test fun onlySuccessfulAdditionOpensCanonicalRoasterTargetAndCancelDoesNotDeleteIt() = runTest {
        val sessions = CheckInTestSessions()
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val writer = Writer()
        val adapter = CollectionPromptRoasterFavorites(writer, sessions, requests)
        adapter.setFavorite(roaster, true).getOrThrow()
        assertEquals(CollectionTarget(CollectionTargetKind.Roaster, "canonical"), requests.state.value?.target)
        requests.dismiss(assertNotNull(requests.state.value))
        assertEquals(listOf(true), writer.writes)
        adapter.setFavorite(roaster, false).getOrThrow()
        assertNull(requests.state.value)
        writer.result = Result.failure(IllegalStateException("Offline"))
        assertTrue(adapter.setFavorite(roaster, true).isFailure)
        assertNull(requests.state.value)
    }

    @Test fun anAccountSwitchDuringTheRoasterWriteNeverShowsAnotherUsersPicker() = runTest {
        val sessions = CheckInTestSessions()
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val writer = Writer().apply { afterWrite = { sessions.applySession(Session("other", userId = "other")) } }
        val adapter = CollectionPromptRoasterFavorites(writer, sessions, requests)
        assertFailsWith<CancellationException> { adapter.setFavorite(roaster, true) }
        assertNull(requests.state.value)
    }
}
