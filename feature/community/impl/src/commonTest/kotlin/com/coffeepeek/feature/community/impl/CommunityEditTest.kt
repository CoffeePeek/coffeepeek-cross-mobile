package com.coffeepeek.feature.community.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.model.FeedPage
import com.coffeepeek.feature.community.domain.model.MyPostsPage
import com.coffeepeek.feature.community.domain.model.PostDrink
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.rules.EditIssue
import com.coffeepeek.feature.community.domain.rules.ReportIssue
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityEvent
import com.coffeepeek.feature.community.impl.timeline.CommunityTimeline
import com.coffeepeek.feature.community.impl.timeline.CommunityViewModel
import com.coffeepeek.feature.community.impl.timeline.EditError
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

/** Editing, hiding and reporting: the second half of the legacy timeline behaviour. */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityEditTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val own = post("own", slug = "me").copy(drink = PostDrink("cappuccino", null, "Капучино", "Cappuccino"), rating = PostRating(4, 4, 5), note = "Original")
    private val other = post("other")

    private fun TestScope.model(
        repository: FakeRepository = FakeRepository(), account: FakeAccount = FakeAccount("one"), analytics: FakeAnalytics = FakeAnalytics(), drinks: FakeDrinks = FakeDrinks(),
    ) = CommunityViewModel(repository, account, analytics, drinks, FakeAuthors()).also { store.put("community", it); runCurrent() }

    private fun TestScope.events(vm: CommunityViewModel) = mutableListOf<CommunityEvent>().also { list -> backgroundScope.launch { vm.events.collect { list += it } } }
    private fun CommunityViewModel.send(action: CommunityAction, scope: TestScope) { onAction(action); scope.runCurrent() }
    private fun publicRepo(update: suspend (String, PostEdit) -> Result<CommunityPost> = { _, _ -> error("Unused") }) = FakeRepository().apply {
        feed = { _, _, _ -> Result.success(FeedPage(listOf(entry(own), entry(other)), "next")) }
        this.update = update
    }

    @Test fun onlyAnOwnPostOpensTheEditorAndItLoadsTheDrinksAndReportsTheFunnelStart() = runTest(dispatcher) {
        val drinks = FakeDrinks(); val analytics = FakeAnalytics()
        val vm = model(publicRepo(), analytics = analytics, drinks = drinks)
        vm.send(CommunityAction.Edit("other"), this); assertNull(vm.state.value.editing)
        vm.send(CommunityAction.Edit("missing"), this); assertNull(vm.state.value.editing)
        vm.send(CommunityAction.Edit("own"), this)
        assertEquals("own", vm.state.value.editing?.source?.id)
        assertEquals(2, vm.state.value.drinks.size); assertEquals(1, drinks.loads)
        assertEquals("Original", vm.state.value.editing?.text); assertEquals("cappuccino", vm.state.value.editing?.drinkSlug)
        assertEquals(listOf("edit:opened"), analytics.events)
    }

    @Test fun editingIsIgnoredWhileAReportIsOpenOrAMutationRuns() = runTest(dispatcher) {
        val vm = model(publicRepo())
        vm.send(CommunityAction.OpenReport("other"), this)
        vm.send(CommunityAction.Edit("own"), this); assertNull(vm.state.value.editing)
        vm.send(CommunityAction.DismissReport, this)
        vm.send(CommunityAction.Edit("own"), this); assertEquals("own", vm.state.value.editing?.source?.id)
        vm.send(CommunityAction.OpenReport("other"), this); assertNull(vm.state.value.reportingId) // not while editing
    }

    @Test fun changesReportTheirFieldInTheLegacyOrderAndCancelClosesTheEditor() = runTest(dispatcher) {
        val analytics = FakeAnalytics(); val vm = model(publicRepo(), analytics = analytics)
        vm.send(CommunityAction.Edit("own"), this)
        vm.send(CommunityAction.EditText("Changed"), this); vm.send(CommunityAction.EditText("Changed"), this) // no change, no event
        vm.send(CommunityAction.EditRating(PostRating(5, 5, 5)), this)
        vm.send(CommunityAction.EditDrink("other", ""), this)
        vm.send(CommunityAction.DismissEdit, this)
        assertEquals(listOf("edit:opened", "edit:field:Note", "edit:field:Rating", "edit:field:Drink", "edit:cancelled"), analytics.events)
        assertNull(vm.state.value.editing)
    }

    @Test fun anInvalidFormIsRejectedBeforeAnyRequestAndAnEditClearsTheError() = runTest(dispatcher) {
        var updates = 0; val analytics = FakeAnalytics()
        val vm = model(publicRepo { _, _ -> updates++; error("Unused") }, analytics = analytics)
        vm.send(CommunityAction.Edit("own"), this)
        vm.send(CommunityAction.EditText("x".repeat(1001)), this); vm.send(CommunityAction.SaveEdit, this)
        assertEquals(EditError.Invalid(EditIssue.NoteTooLong), vm.state.value.editing?.error); assertEquals(0, updates)
        assertEquals(listOf("edit:opened", "edit:field:Note", "edit:submitted", "edit:rejected"), analytics.events)
        vm.send(CommunityAction.EditText("short"), this); assertNull(vm.state.value.editing?.error)
        vm.send(CommunityAction.EditDrink("other", " "), this); vm.send(CommunityAction.SaveEdit, this)
        assertEquals(EditError.Invalid(EditIssue.DrinkNameRequired), vm.state.value.editing?.error)
    }

    @Test fun savingSendsTrimmedValuesKeepsTheDrinkAndAPublicPostLeavesThePublicFeedWithAReviewNotice() = runTest(dispatcher) {
        val sent = mutableListOf<PostEdit>(); val release = CompletableDeferred<Unit>()
        val pending = own.copy(note = "Edited", moderation = PostModeration.Pending, contentRevision = 2)
        val analytics = FakeAnalytics()
        val vm = model(publicRepo { id, edit -> assertEquals("own", id); sent += edit; release.await(); Result.success(pending) }, analytics = analytics)
        val events = events(vm)
        vm.send(CommunityAction.Edit("own"), this)
        vm.send(CommunityAction.EditText("  Edited "), this)
        vm.send(CommunityAction.SaveEdit, this)
        assertTrue(vm.state.value.isSaving)
        vm.send(CommunityAction.SaveEdit, this); vm.send(CommunityAction.DismissEdit, this) // ignored while saving
        assertEquals(1, sent.size); assertTrue(vm.state.value.editing != null)
        release.complete(Unit); runCurrent()
        assertEquals(PostEdit("Edited", PostRating(4, 4, 5), "cappuccino", null), sent.single())
        assertEquals(listOf("other"), vm.state.value.items.map { it.id }); assertNull(vm.state.value.editing); assertFalse(vm.state.value.isSaving)
        assertFalse("own" in vm.state.value.publishedAt)
        assertEquals(MessageKind.EditSentForReview, assertIs<CommunityEvent.Message>(events.single()).message.kind)
        assertEquals("edit:saved:Public:false", analytics.events.last())
    }

    @Test fun onThePersonalTimelineTheEditedCardIsReplacedInPlaceWithASimpleNotice() = runTest(dispatcher) {
        val pending = own.copy(note = "Edited", moderation = PostModeration.Pending)
        val repo = FakeRepository().apply {
            mine = { _, _ -> Result.success(MyPostsPage(listOf(own), false)) }
            update = { _, _ -> Result.success(pending) }
        }
        val vm = model(repo); val events = events(vm)
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this)
        vm.send(CommunityAction.Edit("own"), this); vm.send(CommunityAction.SaveEdit, this)
        assertEquals(listOf(pending), vm.state.value.items)
        assertEquals(MessageKind.EditSaved, assertIs<CommunityEvent.Message>(events.single()).message.kind)
    }

    @Test fun aFailedSaveKeepsTheDraftWithTheServerMessageOrTheDefaultAndReportsTheFailure() = runTest(dispatcher) {
        var round = 0; val analytics = FakeAnalytics()
        val vm = model(publicRepo { _, _ ->
            if (round++ == 0) Result.failure(CommunityFailure(FailureKind.Other, "Server says no")) else Result.failure(IllegalStateException("Offline"))
        }, analytics = analytics)
        vm.send(CommunityAction.Edit("own"), this); vm.send(CommunityAction.EditText("Draft"), this)
        vm.send(CommunityAction.SaveEdit, this)
        assertEquals("Draft", vm.state.value.editing?.text); assertEquals(EditError.Server("Server says no"), vm.state.value.editing?.error)
        assertFalse(vm.state.value.isSaving); assertEquals(2, vm.state.value.items.size)
        vm.send(CommunityAction.SaveEdit, this)
        assertEquals(EditError.Server(null), vm.state.value.editing?.error) // a transport failure shows the default text, not its raw message
        assertEquals(2, analytics.events.count { it == "edit:failed" })
    }

    @Test fun aFailedDrinkCatalogCanBeRetried() = runTest(dispatcher) {
        val drinks = FakeDrinks().apply { load = { Result.failure(IllegalStateException("Offline")) } }
        val vm = model(publicRepo(), drinks = drinks)
        vm.send(CommunityAction.Edit("own"), this); assertTrue(vm.state.value.drinksFailed)
        drinks.load = { Result.success(emptyList()) }
        vm.send(CommunityAction.LoadDrinks, this); assertFalse(vm.state.value.drinksFailed)
    }

    @Test fun aSessionChangeClosesTheEditorAndALateSaveForThePreviousAccountIsDropped() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>(); val account = FakeAccount("one")
        val vm = model(publicRepo { _, _ -> release.await(); Result.success(own.copy(note = "Late")) }, account)
        vm.send(CommunityAction.Edit("own"), this); vm.send(CommunityAction.SaveEdit, this)
        account.switchTo(null); runCurrent()
        assertNull(vm.state.value.editing); assertTrue(vm.state.value.drinks.isEmpty()); assertFalse(vm.state.value.isSaving)
        release.complete(Unit); runCurrent()
        assertTrue(vm.state.value.items.none { it.note == "Late" })
    }

    @Test fun aVisibilityFailureThatIsNotAServerAnswerCarriesNoServerMessage() = runTest(dispatcher) {
        val repo = publicRepo().apply { visibility = { _, _ -> Result.failure(IllegalStateException("socket closed: secret detail")) } }
        val vm = model(repo); val events = events(vm)
        vm.send(CommunityAction.ToggleVisibility("own"), this)
        val failed = assertIs<CommunityEvent.Message>(events.single())
        assertEquals(MessageKind.VisibilityFailed, failed.message.kind)
        assertNull(failed.message.serverMessage)
        assertEquals(2, vm.state.value.items.size); assertNull(vm.state.value.changingVisibilityId)
    }

    @Test fun hidingAnOwnPublicPostMakesItPrivateAndRemovesItFromTheFeedAndAFailureKeepsItWithAMessage() = runTest(dispatcher) {
        var fail = true
        val repo = publicRepo().apply { visibility = { id, next ->
            assertEquals(PostVisibility.Private, next)
            if (fail) { fail = false; Result.failure(CommunityFailure(FailureKind.Other, "Server says no")) } else Result.success(own.copy(id = id, visibility = next, moderation = PostModeration.NotSubmitted))
        } }
        val vm = model(repo); val events = events(vm)
        vm.send(CommunityAction.ToggleVisibility("other"), this); assertNull(vm.state.value.changingVisibilityId) // not own
        vm.send(CommunityAction.ToggleVisibility("own"), this)
        val failed = assertIs<CommunityEvent.Message>(events.single())
        assertEquals(MessageKind.VisibilityFailed, failed.message.kind); assertEquals("Server says no", failed.message.serverMessage)
        assertEquals(2, vm.state.value.items.size); assertNull(vm.state.value.changingVisibilityId)
        vm.send(CommunityAction.ToggleVisibility("own"), this)
        assertEquals(listOf("other"), vm.state.value.items.map { it.id })
    }

    @Test fun publishingAPrivatePostOnThePersonalTimelineReplacesItInPlace() = runTest(dispatcher) {
        val private = own.copy(visibility = PostVisibility.Private, moderation = PostModeration.NotSubmitted)
        val repo = FakeRepository().apply {
            mine = { _, _ -> Result.success(MyPostsPage(listOf(private), false)) }
            visibility = { _, next -> assertEquals(PostVisibility.Public, next); Result.success(private.copy(visibility = next, moderation = PostModeration.Pending)) }
        }
        val vm = model(repo)
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this)
        vm.send(CommunityAction.ToggleVisibility("own"), this)
        assertEquals(PostModeration.Pending, vm.state.value.items.single().moderation)
        assertEquals(PostVisibility.Public, vm.state.value.items.single().visibility)
    }

    @Test fun aGuestAsksToSignInToReportAndOwnPostsCannotBeReported() = runTest(dispatcher) {
        val guest = model(publicRepo(), FakeAccount(null)); val events = events(guest)
        guest.send(CommunityAction.OpenReport("other"), this)
        assertEquals(listOf<CommunityEvent>(CommunityEvent.SignInRequested), events); assertNull(guest.state.value.reportingId)
        val vm = model(publicRepo())
        vm.send(CommunityAction.OpenReport("own"), this); assertNull(vm.state.value.reportingId)
    }

    @Test fun reportTextIsCutBlankIsRejectedSuccessClosesTheSheetAndAFailureKeepsTheDraft() = runTest(dispatcher) {
        val sent = mutableListOf<Pair<String, String>>(); var fail = true
        val repo = publicRepo().apply { report = { id, text ->
            sent += id to text
            if (fail) { fail = false; Result.failure(CommunityFailure(FailureKind.Other, "Try later")) } else Result.success(Unit)
        } }
        val vm = model(repo); val events = events(vm)
        vm.send(CommunityAction.OpenReport("other"), this); assertEquals("other", vm.state.value.reportingId)
        vm.send(CommunityAction.ReportText("x".repeat(2500)), this); assertEquals(2000, vm.state.value.reportText.length)
        vm.send(CommunityAction.ReportText("   "), this); vm.send(CommunityAction.SubmitReport, this)
        assertEquals(ReportIssue.Empty, vm.state.value.reportIssue); assertTrue(sent.isEmpty())
        vm.send(CommunityAction.ReportText(" Reason "), this); assertNull(vm.state.value.reportIssue)
        vm.send(CommunityAction.SubmitReport, this)
        assertTrue(vm.state.value.reportFailed); assertEquals("Try later", vm.state.value.reportFailure)
        assertEquals(" Reason ", vm.state.value.reportText); assertEquals("other", vm.state.value.reportingId)
        vm.send(CommunityAction.SubmitReport, this)
        assertNull(vm.state.value.reportingId); assertEquals("", vm.state.value.reportText)
        assertEquals(listOf("other" to "Reason", "other" to "Reason"), sent) // the text is trimmed before it is sent
        assertEquals(MessageKind.ReportSent, assertIs<CommunityEvent.Message>(events.single()).message.kind)
    }

    @Test fun refreshFiltersAndTimelineAreIgnoredWhileAnEditorOrReportIsOpen() = runTest(dispatcher) {
        val repo = publicRepo(); val vm = model(repo)
        vm.send(CommunityAction.Edit("own"), this)
        val requests = repo.feedRequests.size
        vm.send(CommunityAction.SetFilters(com.coffeepeek.feature.community.domain.model.FeedFilters(citySlug = "x")), this)
        vm.send(CommunityAction.SelectTimeline(CommunityTimeline.Mine), this)
        vm.send(CommunityAction.ToggleHelpful("other"), this)
        assertEquals(requests, repo.feedRequests.size); assertEquals(CommunityTimeline.Public, vm.state.value.timeline); assertNull(vm.state.value.helpfulId)
    }
}
