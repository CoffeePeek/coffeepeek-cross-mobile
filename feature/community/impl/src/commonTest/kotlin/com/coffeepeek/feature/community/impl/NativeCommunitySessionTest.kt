package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.domain.model.FeedPage
import com.coffeepeek.feature.community.impl.post.PostAction
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeCommunitySessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val feeds = mutableListOf<NativeCommunityFeedSession>()
    private val posts = mutableListOf<NativeCommunityPostSession>()

    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() {
        feeds.forEach { it.close() }
        posts.forEach { it.close() }
        Dispatchers.resetMain()
    }

    private fun feed(repository: FakeRepository = FakeRepository(), account: FakeAccount = FakeAccount("one")) =
        NativeCommunityFeedSession(repository, account, FakeAnalytics(), FakeDrinks(), FakeAuthors()).also(feeds::add)

    private fun detail(repository: FakeRepository) = NativeCommunityPostSession(
        "post", repository, FakeAccount("one"), FakeAuthors(), FakeAnalytics(), FakeDrinks(),
    ).also(posts::add)

    @Test fun feedOnlyRefreshesOnceAfterTheExternalHomeEditorReturns() = runTest(dispatcher) {
        val repository = FakeRepository()
        val session = feed(repository)
        runCurrent()
        val initial = repository.feedRequests.size
        session.resumed()
        runCurrent()
        assertEquals(initial, repository.feedRequests.size)
        session.homeEditorOpened()
        session.resumed()
        session.resumed()
        runCurrent()
        assertEquals(initial + 1, repository.feedRequests.size)
    }

    @Test fun feedCloseCancelsItsRequestAndRejectsLaterActionsAndAccountChanges() = runTest(dispatcher) {
        var cancelled = false
        val repository = FakeRepository().apply {
            feed = { _, _, _ -> try { awaitCancellation() } finally { cancelled = true } }
        }
        val account = FakeAccount("one")
        val session = feed(repository, account)
        runCurrent()
        session.close()
        session.close()
        session.perform(CommunityAction.Refresh())
        session.homeEditorOpened()
        session.resumed()
        account.switchTo("two")
        runCurrent()
        assertTrue(cancelled)
        assertEquals(1, repository.feedRequests.size)
    }

    @Test fun sameAccountEmissionKeepsFeedStateAndReplacementUsesExistingEpochRules() = runTest(dispatcher) {
        val account = FakeAccount("one")
        val repository = FakeRepository().apply { feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(post())), null)) } }
        val session = feed(repository, account)
        runCurrent()
        val initial = session.model.state.value
        account.switchTo("one")
        runCurrent()
        assertEquals(initial, session.model.state.value)
        account.switchTo("two")
        runCurrent()
        assertEquals(initial.accountEpoch + 1, session.model.state.value.accountEpoch)
    }

    @Test fun postReloadsOnResumeButNotWhileItsDraftOwnsInteraction() = runTest(dispatcher) {
        val repository = FakeRepository().apply { byId = { Result.success(post(slug = "me")) } }
        val session = detail(repository)
        runCurrent()
        val initial = repository.postRequests.size
        session.resumed()
        runCurrent()
        assertEquals(initial + 1, repository.postRequests.size)
        session.perform(PostAction.Edit)
        runCurrent()
        session.resumed()
        runCurrent()
        assertEquals(initial + 1, repository.postRequests.size)
        assertNotNull(session.model.state.value.editing)
    }

    @Test fun postCloseCancelsPendingReadAndRejectsResume() = runTest(dispatcher) {
        var cancelled = false
        val repository = FakeRepository().apply {
            byId = { try { awaitCancellation() } finally { cancelled = true } }
        }
        val session = detail(repository)
        runCurrent()
        session.close()
        session.close()
        session.resumed()
        session.perform(PostAction.Retry)
        runCurrent()
        assertTrue(cancelled)
        assertEquals(listOf("post"), repository.postRequests)
    }
}
