package com.coffeepeek.feature.collections.impl

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.SavedTestAccount
import com.coffeepeek.feature.collections.impl.ui.SavedTestSnapshots
import com.coffeepeek.feature.collections.impl.ui.savedShop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PublicCollectionSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val account = SavedTestAccount()
    private val snapshots = SavedTestSnapshots()
    private val sessions = mutableListOf<PublicCollectionSession>()
    private val events = mutableListOf<String>()

    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() {
        sessions.forEach(PublicCollectionSession::close)
        Dispatchers.resetMain()
    }

    @Test fun oldSnapshotLinksKeepTheirReadOnlyPlacesAndSharing() = runTest(dispatcher) {
        val session = session(SharedCollection("snapshot", "Title", "https://fixture.invalid", listOf(savedShop)))
        runCurrent()
        assertEquals(listOf(savedShop.key), session.current().places.map { it.key })
        assertEquals("Snapshot", session.current().mode)
        session.openPlace(savedShop.key)
        session.shareLink()
        runCurrent()
        assertEquals(listOf("shop:shop", "Title\nhttps://fixture.invalid"), events)
        assertEquals(0, account.loads)
    }

    @Test fun mutableListLinksHandOffExactlyOnceToSavedPublic() = runTest(dispatcher) {
        val session = session(SharedCollection("list", "Title", "", listOf(savedShop), SharedCollectionMode.List))
        runCurrent()
        assertEquals(listOf("list:list"), events)
        assertTrue(session.current().places.isEmpty())
        assertNull(session.poster())
        session.shareLink()
        session.shareImages()
        session.format(true)
        runCurrent()
        assertEquals(listOf("list:list"), events)
    }

    private fun session(value: SharedCollection): PublicCollectionSession {
        val repository = object : CollectionsRepository by snapshots {
            override suspend fun read(id: String) = Result.success(value)
        }
        return PublicCollectionSession(value.id, repository, account, { events.add("back") },
            { events.add("list:$it") }, { events.add("shop:$it") }, { events.add("roaster:$it") },
            events::add).also(sessions::add)
    }
}

