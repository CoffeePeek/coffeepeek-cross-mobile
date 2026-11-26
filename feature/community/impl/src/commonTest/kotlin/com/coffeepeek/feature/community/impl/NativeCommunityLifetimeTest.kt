package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeCommunityLifetimeTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<NativeCommunityLifetime>()
    private val release = mutableListOf<() -> Unit>()
    private val host = CommunityNativeHost(emptyFlow(), { it }, { it }, {}, {})
    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() {
        release.forEach { it() }
        owners.forEach { it.close() }
        Dispatchers.resetMain()
    }
    private fun lifetime(account: CommunityAccount = FakeAccount("one")) = NativeCommunityLifetime(account, host).also(owners::add)

    @Test fun oneTickerOnlyRunsWhileTheControllerIsStarted() = runTest(dispatcher) {
        val owner = lifetime()
        owner.start { owner.close() }
        runCurrent()
        assertEquals(0L, owner.clock.value)
        owner.presentation(true, 91.0)
        runCurrent()
        assertEquals(1L, owner.clock.value)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2L, owner.clock.value)
        owner.presentation(false, 91.0)
        runCurrent()
        advanceTimeBy(120_000)
        assertEquals(2L, owner.clock.value)
    }

    @Test fun accountReplacementRejectsActionsBeforeItsCollectorAndReleasesOnce() = runTest(dispatcher) {
        val account = FakeAccount("one")
        val owner = lifetime(account)
        var releases = 0
        owner.start {
            releases++
            owner.close()
        }
        runCurrent()
        account.switchTo("two")
        assertFalse(owner.isCurrent())
        runCurrent()
        account.switchTo("one")
        runCurrent()
        assertFalse(owner.isCurrent())
        assertEquals(1, releases)
    }

    @Test fun accountObservationFailureClosesTheLifetime() = runTest(dispatcher) {
        val account = object : CommunityAccount by FakeAccount("one") {
            override val accountKey: Flow<String?> = flow { error("Read failed") }
        }
        val owner = lifetime(account)
        owner.start { owner.close() }
        runCurrent()
        assertFalse(owner.isCurrent())
        assertNull(owner.resources.value)
    }

    @Test fun closeRejectsLaterPresentationAndCancelsItsTicker() = runTest(dispatcher) {
        val owner = lifetime()
        owner.start { owner.close() }
        owner.presentation(true, 50.0)
        runCurrent()
        owner.close()
        owner.presentation(true, 99.0)
        advanceTimeBy(120_000)
        assertFalse(owner.runtime.value.started)
        assertEquals(0.0, owner.runtime.value.bottomClearance)
        assertEquals(1L, owner.clock.value)
    }

    @Test fun notYetPreparedFeedIsActiveAndAccountReplacementClosesItEvenWhileHidden() = runTest(dispatcher) {
        val account = FakeAccount("one")
        val blocks = object : com.coffeepeek.feature.authorblocking.api.AuthorBlockActions {
            override fun requestBlock(author: com.coffeepeek.feature.authorblocking.api.AuthorReference) {}
            override fun requestUnblock(author: com.coffeepeek.feature.authorblocking.api.AuthorReference) {}
        }
        val feed = NativeCommunityFeed(FakeRepository(), account, FakeAnalytics(), FakeDrinks(), FakeAuthors(),
            CommunitySavedPosts.None, blocks, host, CommunityNavigation({}, { _, _, _ -> }, {}, {}, {}, {}))
        release += feed::close
        runCurrent()
        assertTrue(feed.active.value)
        assertNull(feed.state.value)
        account.switchTo("two")
        runCurrent()
        assertFalse(feed.active.value)
        assertNull(feed.state.value)
    }
}
