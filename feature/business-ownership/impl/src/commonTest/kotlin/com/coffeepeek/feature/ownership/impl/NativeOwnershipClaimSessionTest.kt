package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeOwnershipClaimSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val account = ClaimTestAccount()
    private val sessions = mutableListOf<IosOwnershipSession>()
    private var closes = 0
    private var logins = 0
    private var photoDelivery: ((List<NativeOwnershipPhoto>) -> Unit)? = null

    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { sessions.forEach(IosOwnershipSession::close); Dispatchers.resetMain() }

    @Test fun nativeCloseIsDeliveredOnceAndDisposedSessionCannotAct() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        val generation = session.current().generation
        session.action("back", generation)
        session.action("back", generation)
        runCurrent()
        assertEquals(1, closes)
        session.close()
        session.action("login", generation)
        session.claimText("Contact", "late", generation)
        runCurrent()
        assertEquals(0, logins)
        assertFalse(session.current().signedIn)
        assertEquals("", session.current().contact)
    }

    @Test fun accountReplacementClosesClaimAndRejectsPendingPhotoDelivery() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        session.choosePhotos(session.current().generation)
        assertNotNull(photoDelivery)
        account.identity.value = "second"
        runCurrent()
        photoDelivery?.invoke(listOf(NativeOwnershipPhoto("photo.jpg", "image/jpeg", byteArrayOf(1))))
        runCurrent()
        assertEquals(1, closes)
        assertTrue(session.current().evidencePhotos.isEmpty())
    }

    @Test fun cancelledNativePhotoPickerDoesNotAddEvidence() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        session.choosePhotos(session.current().generation)
        photoDelivery?.invoke(emptyList())
        runCurrent()
        assertTrue(session.current().evidencePhotos.isEmpty())
        assertEquals(0, closes)
    }

    @Test fun latePhotoCompletionAfterDisposalCannotRestorePrivateEvidence() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        session.choosePhotos(session.current().generation)
        val receive = assertNotNull(photoDelivery)
        session.close()
        receive(listOf(NativeOwnershipPhoto("photo.jpg", "image/jpeg", byteArrayOf(1))))
        runCurrent()
        assertTrue(session.current().evidencePhotos.isEmpty())
        assertFalse(session.current().signedIn)
    }

    private fun session(): IosOwnershipSession {
        val entry = createBusinessOwnershipEntry(NoClaimWrites, account, image = { _, _, _, _ -> },
            picker = { _, _, content -> content {} }, backHandler = {},
            checkInLink = { null }, shareCheckInQr = { _, _ -> })
        return createIosOwnershipClaimSession(entry, "CoffeeShop", "fixture", { _, receive -> photoDelivery = receive },
            { closes++ }, { logins++ }).also(sessions::add)
    }
}

private class ClaimTestAccount : OwnershipAccount {
    val identity = MutableStateFlow<String?>("first")
    override fun current() = identity.value
    override fun observe() = identity
}

private object NoClaimWrites : BusinessOwnershipRepository {
    override suspend fun claims(page: Int): Result<BusinessPage<OwnershipClaim>> = error("Unexpected read")
    override suspend fun claim(id: String): Result<OwnershipClaim> = error("Unexpected read")
    override suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim> = error("Unexpected write")
    override suspend fun businesses(kind: BusinessKind, page: Int): Result<BusinessPage<OwnedBusiness>> = error("Unexpected read")
    override suspend fun profile(kind: BusinessKind, id: String): Result<BusinessProfile> = error("Unexpected read")
    override suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile> = error("Unexpected write")
}
