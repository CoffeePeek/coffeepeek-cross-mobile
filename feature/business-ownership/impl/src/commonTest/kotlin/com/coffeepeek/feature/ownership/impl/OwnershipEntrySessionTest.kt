package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OwnershipEntrySessionTest {
    private val account = NativeAccessAccount()
    private val repository = NativeAccessRepository()
    private val entry = createBusinessOwnershipEntry(repository, account, image = { _, _, _, _ -> },
        picker = { _, _, content -> content {} }, backHandler = {}, checkInLink = { null }, shareCheckInQr = { _, _ -> })

    @Test fun guestObservationAndResumeDoNotReadPrivateBusinesses() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { entry.observeSessionChanges() }
        entry.refreshSession()
        assertEquals(0, repository.reads)
        assertFalse(entry.accountSwitchState.value.hasBusinesses)
    }

    @Test fun unchangedAccountKeepsBusinessModeAndReplacementRevokesIt() = runTest {
        account.identity.value = "first"
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { entry.observeSessionChanges() }
        entry.switchAccountMode()
        assertTrue(entry.accountSwitchState.value.businessMode)
        val reads = repository.reads
        account.identity.value = "first"
        runCurrent()
        assertEquals(reads, repository.reads)
        assertTrue(entry.accountSwitchState.value.businessMode)
        account.identity.value = null
        runCurrent()
        assertFalse(entry.accountSwitchState.value.businessMode)
        assertTrue(entry.accountSwitchState.value.ownedIdentities.isEmpty())
    }

    @Test fun cancelledRootObserverDoesNotStartReadsForAnotherAccount() = runTest {
        val observation = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { entry.observeSessionChanges() }
        observation.cancel()
        account.identity.value = "next"
        runCurrent()
        assertEquals(0, repository.reads)
    }

}

private class NativeAccessAccount : OwnershipAccount {
    val identity = MutableStateFlow<String?>(null)
    override fun current() = identity.value
    override fun observe() = identity
}

private class NativeAccessRepository : BusinessOwnershipRepository {
    var reads = 0
    override suspend fun businesses(kind: BusinessKind, page: Int): Result<BusinessPage<OwnedBusiness>> {
        reads++
        return Result.success(BusinessPage(listOf(OwnedBusiness("owned", kind, "Owned")), 1, page, 20))
    }
    override suspend fun claims(page: Int): Result<BusinessPage<OwnershipClaim>> = error("Unused")
    override suspend fun claim(id: String): Result<OwnershipClaim> = error("Unused")
    override suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim> = error("Unexpected write")
    override suspend fun profile(kind: BusinessKind, id: String): Result<BusinessProfile> = error("Unused")
    override suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile> = error("Unexpected write")
}
