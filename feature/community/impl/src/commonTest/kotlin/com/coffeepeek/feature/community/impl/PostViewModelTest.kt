package com.coffeepeek.feature.community.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.rules.ReportIssue
import com.coffeepeek.feature.community.impl.post.PostAction
import com.coffeepeek.feature.community.impl.post.PostError
import com.coffeepeek.feature.community.impl.post.PostEvent
import com.coffeepeek.feature.community.impl.post.PostViewModel
import com.coffeepeek.feature.community.impl.timeline.MessageKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun repository(shown: CommunityPost = post("shared")) = FakeRepository().apply { byId = { Result.success(shown) } }

    private fun TestScope.model(repository: FakeRepository = repository(), account: FakeAccount = FakeAccount("one"), authors: FakeAuthors = FakeAuthors()) =
        PostViewModel("shared", repository, account, authors).also { store.put("post", it); runCurrent() }

    private fun TestScope.events(vm: PostViewModel) = mutableListOf<PostEvent>().also { list -> backgroundScope.launch { vm.events.collect { list += it } } }
    private fun PostViewModel.send(action: PostAction, scope: TestScope) { onAction(action); scope.runCurrent() }

    @Test fun aGuestReadsThePostButALikeOrAReportAsksToSignInWithoutARequest() = runTest(dispatcher) {
        val repo = repository().apply { helpful = { _, _ -> error("A guest must not vote") } }
        val vm = model(repo, FakeAccount(null)); val events = events(vm)
        assertEquals("shared", vm.state.value.post?.id); assertEquals(false, vm.state.value.isLoggedIn); assertEquals(listOf("shared"), repo.postRequests)
        vm.send(PostAction.ToggleHelpful, this); vm.send(PostAction.OpenReport, this)
        assertEquals<List<PostEvent>>(listOf(PostEvent.SignInRequested, PostEvent.SignInRequested), events)
        assertFalse(vm.state.value.isReportOpen)
    }

    @Test fun aHiddenPostIsUnavailableWhileAFailedReadCanBeRetried() = runTest(dispatcher) {
        assertEquals(PostError.Unavailable, model(FakeRepository().apply { byId = { Result.failure(CommunityFailure(FailureKind.NotFound)) } }).state.value.error)
        var attempts = 0
        val repo = FakeRepository().apply { byId = { if (attempts++ == 0) Result.failure(IllegalStateException("offline")) else Result.success(post("shared")) } }
        val vm = model(repo)
        assertEquals(PostError.Failed, vm.state.value.error); assertNull(vm.state.value.post)
        vm.send(PostAction.Retry, this)
        assertNull(vm.state.value.error); assertEquals("shared", vm.state.value.post?.id)
    }

    @Test fun anotherAccountReloadsThePostAndDropsTheLateAnswerForThePreviousOne() = runTest(dispatcher) {
        val first = CompletableDeferred<Result<CommunityPost>>()
        var calls = 0
        val repo = FakeRepository().apply { byId = { if (calls++ == 0) first.await() else Result.success(post("shared", note = "second account")) } }
        val account = FakeAccount("one")
        val vm = model(repo, account)
        account.switchTo("two"); runCurrent()
        first.complete(Result.success(post("shared", note = "first account"))); runCurrent()
        assertEquals("second account", vm.state.value.post?.note); assertEquals(2, repo.postRequests.size)
    }

    @Test fun aLikeKeepsTheServerCountAndAFailedVoteShowsTheServerMessage() = runTest(dispatcher) {
        var fail = false
        val repo = repository().apply { helpful = { _, helpful -> if (fail) Result.failure(CommunityFailure(FailureKind.Other, "Нельзя")) else Result.success(HelpfulVote(helpful, 7)) } }
        val vm = model(repo); val events = events(vm)
        vm.send(PostAction.ToggleHelpful, this)
        assertEquals(7, vm.state.value.post?.helpfulCount); assertTrue(vm.state.value.post!!.isHelpfulByCurrentUser); assertFalse(vm.state.value.isVoting)
        fail = true
        vm.send(PostAction.ToggleHelpful, this)
        val message = assertIs<PostEvent.Message>(events.single()).message
        assertEquals(MessageKind.VoteFailed, message.kind); assertEquals("Нельзя", message.serverMessage)
    }

    @Test fun anOwnPostOrOneNotYetPublishedCannotBeLikedOrReported() = runTest(dispatcher) {
        val unused = { repo: FakeRepository -> repo.apply { helpful = { _, _ -> error("Unused") }; report = { _, _ -> error("Unused") } } }
        val own = model(unused(repository(post("shared", slug = "me"))))
        assertTrue(own.state.value.isOwn)
        val pending = model(unused(repository(post("shared").copy(moderation = PostModeration.Pending))))
        val hidden = model(unused(repository(post("shared").copy(visibility = PostVisibility.Private))))
        for (vm in listOf(own, pending, hidden)) {
            vm.send(PostAction.ToggleHelpful, this); vm.send(PostAction.OpenReport, this)
            assertFalse(vm.state.value.isReportOpen); assertFalse(vm.state.value.canBlockAuthor)
        }
    }

    @Test fun blockingTheAuthorHidesThePostButAnUnreadySnapshotDoesNot() = runTest(dispatcher) {
        val authors = FakeAuthors()
        val vm = model(authors = authors)
        assertTrue(vm.state.value.canBlockAuthor)
        authors.visibility.value = AuthorVisibilitySnapshot(revision = 1, ready = false); runCurrent()
        assertEquals("shared", vm.state.value.post?.id)
        authors.visibility.value = AuthorVisibilitySnapshot(revision = 2, blockedIds = setOf("author-shared")); runCurrent()
        assertNull(vm.state.value.post); assertEquals(PostError.Unavailable, vm.state.value.error)
    }

    @Test fun aReportIsValidatedSentOnceAndClosedWithANotice() = runTest(dispatcher) {
        val sent = mutableListOf<Pair<String, String>>()
        val vm = model(repository().apply { report = { id, text -> sent += id to text; Result.success(Unit) } }); val events = events(vm)
        vm.send(PostAction.OpenReport, this)
        vm.send(PostAction.SubmitReport, this)
        assertEquals(ReportIssue.Empty, vm.state.value.reportIssue); assertTrue(sent.isEmpty())
        vm.send(PostAction.ReportText("  Спам  "), this); vm.send(PostAction.SubmitReport, this)
        assertEquals(listOf("shared" to "Спам"), sent); assertFalse(vm.state.value.isReportOpen)
        assertEquals(MessageKind.ReportSent, assertIs<PostEvent.Message>(events.single()).message.kind)
    }
}
