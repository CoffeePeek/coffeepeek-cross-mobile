package com.coffeepeek.feature.ownership.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosOwnershipSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { store.clear(); Dispatchers.resetMain() }

    @Test fun oldGenerationCannotExposePrivateDraftOrPhotosBeforeAccountResetCompletes() = runTest(dispatcher) {
        val account = NativeAccount()
        val session = session(NativeRepository(), account)
        runCurrent()
        val generation = session.current().generation
        session.business("Roaster", "business", generation)
        runCurrent()
        assertNotNull(session.photo("draft:photo", generation))
        account.value.value = "replacement"
        assertTrue(session.current().fields.isEmpty())
        assertNull(session.photo("draft:photo", generation))
        session.profileText("Name", "old private draft", generation)
        runCurrent()
        assertTrue(session.current().fields.isEmpty())
    }

    @Test fun publishConfirmationAndProfileSavedEventStayOwnedByTheSharedModel() = runTest(dispatcher) {
        val repository = NativeRepository()
        val account = NativeAccount()
        val model = model(repository, account)
        val session = IosOwnershipSession(model, account, false, false, {}, { null }, { _, _ -> }, { _, _ -> })
        runCurrent()
        val generation = session.current().generation
        session.business("Roaster", "business", generation)
        runCurrent()
        session.profileText("Name", "Updated", generation)
        runCurrent()
        session.action("save", generation)
        runCurrent()
        assertEquals(0, repository.saves)
        session.action("askSave", generation)
        runCurrent()
        assertTrue(session.current().confirmPublish)
        verifyNativeSaveEvent(model, session, repository, generation)
    }

    @Test fun nativeToolbarReusesAccessAndPreservesEditorIdWithoutLoadingAnotherViewModel() = runTest(dispatcher) {
        val repository = NativeRepository()
        val account = NativeAccount()
        val calls = mutableListOf<String>()
        val entry = entry(repository, account, calls)
        val toolbar = createIosOwnershipActionsSession(entry, "Roaster", "public-slug", "public-slug", {}, { calls += "ownerEdit" })
        try {
            entry.switchAccountMode()
            assertTrue(toolbar.owner())
            toolbar.edit()
            assertEquals(listOf("ownerEdit", "edit:Roaster:business"), calls)
            assertEquals(2, repository.businessReads)
            account.value.value = null
            toolbar.edit()
            assertFalse(toolbar.owner())
            assertFalse(toolbar.requestClaim())
            assertEquals("login", calls.last())
        } finally { toolbar.close() }
    }

    private fun model(repository: NativeRepository, account: NativeAccount) =
        OwnershipViewModel(repository, account).also { store.put("model", it) }
    private fun session(repository: NativeRepository, account: NativeAccount) =
        IosOwnershipSession(model(repository, account), account, false, false, {}, { null }, { _, _ -> }, { _, _ -> })
    private suspend fun TestScope.verifyNativeSaveEvent(model: OwnershipViewModel, session: IosOwnershipSession,
        repository: NativeRepository, generation: Int) {
        val event = async(start = CoroutineStart.UNDISPATCHED) { model.events.first() }
        session.action("save", generation)
        runCurrent()
        assertEquals(1, repository.saves)
        assertTrue(session.current().saved)
        assertEquals("ProfileSaved", event.await().name)
    }
    private fun entry(repository: NativeRepository, account: NativeAccount, calls: MutableList<String>) =
        createBusinessOwnershipEntry(repository, account, { _, _, _, _ -> }, { _, _, content -> content {} }, {},
            { null }, { _, _ -> }, onEdit = { kind, id -> calls += "edit:$kind:$id" }, onLogin = { calls += "login" })
}

private class NativeAccount : OwnershipAccount {
    val value = MutableStateFlow<String?>("owner")
    override fun current() = value.value
    override fun observe() = value
}
private class NativeRepository : BusinessOwnershipRepository {
    var saves = 0
    var businessReads = 0
    private val profile = BusinessProfile("business", BusinessKind.Roaster, 1, "Coffee", photos = listOf(
        BusinessPhoto("photo", fileName = "photo.jpg", bytes = byteArrayOf(1))))
    override suspend fun businesses(kind: BusinessKind, page: Int): Result<BusinessPage<OwnedBusiness>> {
        businessReads++
        return Result.success(BusinessPage(listOf(OwnedBusiness("business", kind, "Coffee", "public-slug")), 1, 1, 20))
    }
    override suspend fun claims(page: Int) = Result.success(BusinessPage<OwnershipClaim>(emptyList(), 0, page, 20))
    override suspend fun claim(id: String): Result<OwnershipClaim> = error("No claim reads from native toolbar")
    override suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim> = error("No native writer")
    override suspend fun profile(kind: BusinessKind, id: String) = Result.success(profile.copy(kind = kind))
    override suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile> {
        saves++
        return Result.success(draft.applyTo(original).copy(revision = 2))
    }
}
