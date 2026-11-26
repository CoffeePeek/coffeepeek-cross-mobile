package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeOwnershipRouteSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val account = RouteAccount()
    private val repository = RouteRepository()
    private val entry = createBusinessOwnershipEntry(repository, account, image = { _, _, _, _ -> },
        picker = { _, _, content -> content {} }, backHandler = {}, checkInLink = { null }, shareCheckInQr = { _, _ -> })
    private val sessions = mutableListOf<IosOwnershipSession>()
    private var closes = 0
    private var disposals = 0
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { sessions.forEach(IosOwnershipSession::close); Dispatchers.resetMain() }
    private fun session() = createIosOwnershipSession(entry, false, "Roaster", null, "owned", { _, _ -> },
        onClose = { closes++ }, onLogin = { error("Unexpected login") }, onDispose = { disposals++ }).also(sessions::add)

    @Test fun savingANativeEditorPublishesTheSharedProfileRevisionWithoutClosingIt() = runTest(dispatcher) {
        val source = session()
        runCurrent()
        val generation = source.current().generation
        source.profileText("Name", "Changed", generation)
        runCurrent()
        source.action("askSave", generation)
        runCurrent()
        source.action("save", generation)
        runCurrent()
        assertEquals(1, repository.saves)
        assertEquals(1L, entry.profileChanges.value)
        assertEquals(0, closes)
    }

    @Test fun accountReplacementClearsTheEditorAndLeavesStackResetToTheRoot() = runTest(dispatcher) {
        val source = session()
        runCurrent()
        val generation = source.current().generation
        account.identity.value = null
        runCurrent()
        source.profileText("Name", "Late", generation)
        source.action("save", generation)
        runCurrent()
        assertFalse(source.current().signedIn)
        assertEquals(0, repository.saves)
        assertEquals(0, closes)
        source.close()
        source.close()
        assertEquals(1, disposals)
    }
}

private class RouteAccount : OwnershipAccount {
    val identity = MutableStateFlow<String?>("owner")
    override fun current() = identity.value
    override fun observe() = identity
}

private class RouteRepository : BusinessOwnershipRepository {
    private val original = BusinessProfile("owned", BusinessKind.Roaster, 1, "Original")
    var saves = 0
    override suspend fun businesses(kind: BusinessKind, page: Int): Result<BusinessPage<OwnedBusiness>> = error("Unused")
    override suspend fun claims(page: Int): Result<BusinessPage<OwnershipClaim>> = error("Unused")
    override suspend fun claim(id: String): Result<OwnershipClaim> = error("Unused")
    override suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim> = error("Unexpected write")
    override suspend fun profile(kind: BusinessKind, id: String) = Result.success(original)
    override suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile> {
        saves++
        return Result.success(original.copy(revision = original.revision + 1, name = draft.name))
    }
}
