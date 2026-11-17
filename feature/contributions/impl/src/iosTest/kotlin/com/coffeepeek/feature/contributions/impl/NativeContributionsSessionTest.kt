package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class NativeContributionsSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { Dispatchers.resetMain() }

    @Test
    fun ownerReplacementBlocksAPreviouslyDisplayedShopBeforeCollectionCatchesUp() = runTest(dispatcher) {
        val account = FakeAccount("first")
        val repository = FakeRepository().apply {
            shops = { status, _ ->
                val owner = account.currentAccountKey().orEmpty()
                if (status == ModerationStatus.Approved) pageOf(shop(owner, published = "shop-$owner")) else pageOf()
            }
        }
        val session = NativeContributionsListSession(ContributionKind.Shops, repository, account)
        try {
            runCurrent()
            assertEquals(true to "shop-first", session.destination("first"))
            account.key.value = "second"
            assertNull(session.destination("first"))
            runCurrent()
            assertEquals(true to "shop-second", session.destination("second"))
        } finally {
            session.close()
            runCurrent()
        }
    }
}
