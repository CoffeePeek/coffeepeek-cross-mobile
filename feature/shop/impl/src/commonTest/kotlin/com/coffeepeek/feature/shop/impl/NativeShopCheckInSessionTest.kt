package com.coffeepeek.feature.shop.impl

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPublication
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPublicationReader
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NativeShopCheckInSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val opened = mutableListOf<NativeShopCheckInSession>()
    private val initial = ShopCheckInCreateInput("shop", "", ShopRating(4, 4, 4), "2026-11-17T09:00:00Z")

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() {
        opened.forEach(NativeShopCheckInSession::close)
        Dispatchers.resetMain()
    }

    @Test fun reopeningAnUncertainDraftCannotWriteOrAttachUntilHistoryIsChecked() = runTest(dispatcher) {
        val repository = Repository()
        val drafts = Drafts(ShopCheckInDraftSnapshot(initial.copy(text = "Saved"), deliveryUnconfirmed = true, exists = true))
        val session = session(repository, drafts)
        runCurrent()
        session.submit()
        session.photosAdded(listOf(photo()))
        runCurrent()
        assertEquals("Saved", session.current().input.text)
        assertTrue(session.current().deliveryUnconfirmed)
        assertFalse(session.current().editable)
        assertEquals(0, repository.writes)
        assertTrue(session.current().input.photos.isEmpty())
        session.historyChecked()
        runCurrent()
        assertTrue(session.current().editable)
        assertFalse(drafts.snapshot!!.deliveryUnconfirmed)
    }

    @Test fun duplicateSubmitStillWritesOnceAndReportsConfirmedPublication() = runTest(dispatcher) {
        val repository = Repository().apply { gate = CompletableDeferred() }
        val session = session(repository)
        val events = mutableListOf<NativeShopCheckInEvent>()
        backgroundScope.launch { session.events.collect { events += it } }
        runCurrent()
        session.submit()
        session.submit()
        runCurrent()
        assertTrue(session.current().submitting)
        assertEquals(1, repository.writes)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertTrue(session.current().submitted)
        assertEquals(ShopCheckInPublication.Published, session.current().publication)
        assertEquals(listOf(NativeShopCheckInEvent.Submitted), events)
        assertFalse(session.current().editable)
    }

    @Test fun uncertainDeliveryStaysInTheDraftAndDoesNotAutomaticallyRetry() = runTest(dispatcher) {
        val repository = Repository().apply { failure = ShopCheckInCreationUnconfirmed() }
        val drafts = Drafts()
        val session = session(repository, drafts)
        runCurrent()
        session.submit()
        runCurrent()
        session.submit()
        runCurrent()
        assertEquals(1, repository.writes)
        assertTrue(session.current().deliveryUnconfirmed)
        assertTrue(drafts.snapshot!!.deliveryUnconfirmed)
        assertFalse(session.current().submitted)
    }

    @Test fun closingTheNativeFormDropsLatePickerActionsAndRetainsTheDraft() = runTest(dispatcher) {
        val drafts = Drafts()
        val session = session(drafts = drafts)
        runCurrent()
        session.noteChanged("Keep this draft")
        runCurrent()
        session.close()
        session.photosAdded(listOf(photo()))
        session.noteChanged("Late callback")
        session.discard()
        runCurrent()
        assertFalse(session.current().active)
        assertEquals("Keep this draft", drafts.snapshot!!.input.text)
        assertTrue(drafts.snapshot!!.input.photos.isEmpty())
    }

    private fun session(repository: Repository = Repository(), drafts: Drafts = Drafts()): NativeShopCheckInSession {
        val publication = object : ShopCheckInPublicationReader {
            override suspend fun status(checkInId: String) = Result.success(ShopCheckInPublication.Published)
        }
        return NativeShopCheckInSession(initial, repository, drafts, publication = publication,
            now = { Instant.parse("2026-11-17T12:00:00Z") }).also(opened::add)
    }

    private fun photo() = ShopCheckInPhoto(byteArrayOf(1, 2), "photo.jpg")

    private class Repository : ShopCheckInRepository {
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        var failure: Throwable? = null
        override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
        override suspend fun create(input: ShopCheckInCreateInput): Result<String> {
            writes++
            gate?.await()
            failure?.let { throw it }
            return Result.success("created")
        }
    }

    private class Drafts(var snapshot: ShopCheckInDraftSnapshot? = null) : ShopCheckInDraftStore {
        override suspend fun open(initial: ShopCheckInCreateInput) = Result.success(snapshot ?: ShopCheckInDraftSnapshot(initial))
        override suspend fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit> {
            this.snapshot = snapshot.copy(exists = true)
            return Result.success(Unit)
        }
        override suspend fun clear(shopSlug: String): Result<Unit> {
            snapshot = null
            return Result.success(Unit)
        }
    }
}
