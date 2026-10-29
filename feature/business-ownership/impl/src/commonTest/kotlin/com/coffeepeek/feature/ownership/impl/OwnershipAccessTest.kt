package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.impl.ui.OwnershipAccess
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class OwnershipAccessTest {
    private class Account : OwnershipAccount {
        val value = MutableStateFlow<String?>("user")
        override fun current() = value.value
        override fun observe() = value
    }
    private class Repository : BusinessOwnershipRepository {
        var currentOwner = true
        var reads = 0
        var beforeResponse: () -> Unit = {}
        override suspend fun businesses(kind: BusinessKind, page: Int): Result<BusinessPage<OwnedBusiness>> {
            reads++
            beforeResponse()
            val items = if (currentOwner) listOf(OwnedBusiness("business", kind, "Name", slug = "public-slug")) else emptyList()
            return Result.success(BusinessPage(items, items.size, page, 20))
        }
        override suspend fun claims(page: Int): Result<BusinessPage<OwnershipClaim>> = error("Claims do not grant access")
        override suspend fun claim(id: String): Result<OwnershipClaim> = error("Unused")
        override suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim> = error("Unused")
        override suspend fun profile(kind: BusinessKind, id: String): Result<BusinessProfile> = error("Unused")
        override suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile> = error("Unused")
    }
    @Test fun guestsNeverReadPrivateBusinesses() = runTest {
        val repo = Repository()
        val access = OwnershipAccess(repo, Account().apply { value.value = null })
        access.refresh()
        access.switchMode()
        assertEquals(0, repo.reads)
        assertFalse(access.state.value.businessMode)
    }
    @Test fun onlyCurrentBindingsEnableModeAndRevocationImmediatelyLeavesIt() = runTest {
        val repo = Repository()
        val access = OwnershipAccess(repo, Account())
        access.refresh()
        assertTrue(access.owns(BusinessKind.CoffeeShop, "business"))
        assertEquals("business", access.editorId(BusinessKind.CoffeeShop, "public-slug", "public-slug"))
        access.switchMode()
        assertTrue(access.state.value.businessMode)
        repo.currentOwner = false
        access.refresh()
        assertFalse(access.state.value.hasBusinesses)
        assertFalse(access.state.value.businessMode)
        assertFalse(access.owns(BusinessKind.CoffeeShop, "business"))
    }
    @Test fun accountChangeDuringAReadCannotPublishOldPermissions() = runTest {
        val repo = Repository()
        val account = Account()
        val access = OwnershipAccess(repo, account)
        repo.beforeResponse = { account.value.value = "other" }
        access.refresh()
        assertTrue(access.state.value.ownedIdentities.isEmpty())
        assertFalse(access.state.value.businessMode)
    }
    @Test fun returningToPersonalModeDoesNotRemoveBusinessAccess() = runTest {
        val access = OwnershipAccess(Repository(), Account())
        access.switchMode()
        access.switchMode()
        assertFalse(access.state.value.businessMode)
        assertTrue(access.state.value.hasBusinesses)
    }
}
