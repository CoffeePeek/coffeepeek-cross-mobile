package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeContributionFormSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val account = FakeAccount()
    private val repository = FakeRepository()
    private val catalogs = FakeCatalogs().apply {
        catalogs = { Result.success(Catalogs(cities = listOf(CatalogOption("city", "City")))) }
    }
    private val location = FakeLocation()
    private val sessions = mutableListOf<NativeContributionFormSession>()

    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() {
        sessions.forEach(NativeContributionFormSession::close)
        Dispatchers.resetMain()
    }

    @Test fun guestFormRejectsNativeEditsPhotosAndSubmit() = runTest(dispatcher) {
        account.key.value = null
        val session = session(NativeContributionFormKind.AddShop)
        runCurrent()
        session.edit("name", "Private")
        session.control("submit")
        assertEquals(-1, session.beginPhotoPick(false))
        runCurrent()
        assertEquals(0, session.current().signedIn)
        assertEquals("", session.current().fields.first { it.key == "name" }.value)
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun shopNativeStepsUseTheSharedValidatorsAndKeepDraftWhenGoingBack() = runTest(dispatcher) {
        val session = session(NativeContributionFormKind.AddShop)
        runCurrent()
        assertFalse(session.current().canSubmit)
        session.edit("name", "Coffee")
        session.edit("address", "Street 1")
        runCurrent()
        assertTrue(session.current().canSubmit)
        session.control("next")
        runCurrent()
        session.edit("email", "invalid")
        runCurrent()
        assertFalse(session.current().canSubmit)
        session.control("next")
        runCurrent()
        assertEquals(1, session.current().step)
        session.control("previous")
        runCurrent()
        assertEquals("Coffee", session.current().fields.first { it.key == "name" }.value)
    }

    @Test fun backFromFailedSubmissionReturnsToAllThreeSharedDrafts() = runTest(dispatcher) {
        repository.shopSubmit = { Result.failure(IllegalStateException("rejected")) }
        repository.roasterSubmit = { Result.failure(IllegalStateException("rejected")) }
        repository.changeSubmit = { Result.failure(IllegalStateException("rejected")) }
        for (kind in listOf(NativeContributionFormKind.AddShop, NativeContributionFormKind.AddRoaster, NativeContributionFormKind.Editor)) {
            val source = failedForm(kind)
            assertEquals("failed", source.current().submit)
            val fields = source.current().fields.map { it.key to it.value }
            assertTrue(source.consumeBack())
            runCurrent()
            assertEquals("idle", source.current().submit)
            assertEquals(fields, source.current().fields.map { it.key to it.value })
        }
    }

    private fun TestScope.failedForm(kind: NativeContributionFormKind): NativeContributionFormSession {
        val source = session(kind)
        runCurrent()
        source.edit("name", "Coffee")
        source.edit("address", "Street 1")
        source.edit("description", "New")
        runCurrent()
        repeat(if (kind == NativeContributionFormKind.AddShop) 5 else 1) {
            source.control("submit")
            runCurrent()
        }
        return source
    }

    @Test fun nativePhotoRequestCopiesBytesAcceptsOneCallbackAndRejectsOldAccount() = runTest(dispatcher) {
        val session = session(NativeContributionFormKind.AddRoaster)
        runCurrent()
        val revision = session.beginPhotoPick(false)
        val input = photo(1)
        session.pickedPhotos(revision, listOf(input))
        input.bytes.fill(9)
        session.pickedPhotos(revision, listOf(photo(2)))
        runCurrent()
        val snapshot = session.current()
        assertEquals(1, snapshot.photos.size)
        assertContentEquals(byteArrayOf(1), session.localPhoto(snapshot.photos.single().key, snapshot.accountVersion))
        val old = session.beginPhotoPick(false)
        account.key.value = "second"
        session.pickedPhotos(old, listOf(photo(3)))
        runCurrent()
        assertTrue(session.current().photos.isEmpty())
    }

    @Test fun nativeEditorSubmitsOnlyItsSectionAndExposesTheReturnedRequest() = runTest(dispatcher) {
        catalogs.shop = { Result.success(ShopSnapshot("cafe", "Cafe", description = "Old")) }
        val session = session(NativeContributionFormKind.Editor, ChangeSection.Description)
        runCurrent()
        session.edit("description", "New")
        session.edit("phone", "123456789")
        session.control("submit")
        runCurrent()
        assertEquals(listOf(ChangeDraft.Description("cafe", "New")), repository.drafts)
        assertEquals("done", session.current().submit)
        assertEquals("new", session.current().requestId)
        assertEquals("Pending", session.current().resultStatus)
    }

    @Test fun staleAndDismissedMapResolutionsCannotReturnToTheForm() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        location.addressGate = gate
        location.address = "Late street"
        val session = session(NativeContributionFormKind.AddShop)
        runCurrent()
        session.control("openMap")
        runCurrent()
        session.moveMap(53.9, 27.5)
        advanceTimeBy(350)
        runCurrent()
        session.control("dismissMap")
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertNull(session.current().map)
        assertEquals("", session.current().fields.first { it.key == "address" }.value)
    }

    @Test fun nativeMapRequiresCurrentAddressAndConfirmsCoordinatesThroughSharedAction() = runTest(dispatcher) {
        location.address = "  Street 1  "
        val session = session(NativeContributionFormKind.AddShop)
        runCurrent()
        session.control("openMap")
        runCurrent()
        session.moveMap(53.9, 27.5)
        session.control("confirmMap")
        runCurrent()
        assertNotNull(session.current().map)
        advanceTimeBy(350)
        runCurrent()
        assertTrue(session.current().map?.canConfirm == true)
        session.control("confirmMap")
        runCurrent()
        assertNull(session.current().map)
        assertEquals("Street 1", session.current().fields.first { it.key == "address" }.value)
    }

    @Test fun anExplicitNativeLocationUpdatesTheSharedMapCameraOnce() = runTest(dispatcher) {
        location.point = GeoPoint(53.9, 27.5)
        val session = session(NativeContributionFormKind.AddShop)
        runCurrent()
        session.control("openMap")
        runCurrent()
        session.locationGranted(map = true)
        runCurrent()
        assertEquals(53.9, session.current().map?.cameraLatitude)
        session.cameraApplied()
        assertNull(session.current().map?.cameraLatitude)
    }

    @Test fun closeDiscardsPendingNativePhotoCallbacks() = runTest(dispatcher) {
        val session = session(NativeContributionFormKind.AddRoaster)
        runCurrent()
        val revision = session.beginPhotoPick(false)
        session.close()
        session.pickedPhotos(revision, listOf(photo(1)))
        runCurrent()
        assertTrue(session.current().photos.isEmpty())
        assertTrue(repository.requests.isEmpty())
    }

    private fun session(kind: NativeContributionFormKind, section: ChangeSection = ChangeSection.Description) =
        NativeContributionFormSession(kind, repository, catalogs, account, location, "cafe", section, null).also(sessions::add)

    private fun photo(value: Int) = PendingPhoto("photo-$value.jpg", "image/jpeg", byteArrayOf(value.toByte()))
}

