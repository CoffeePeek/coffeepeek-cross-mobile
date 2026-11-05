package com.coffeepeek.feature.contentreport.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contentreport.domain.model.*
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import com.coffeepeek.feature.contentreport.impl.ui.*
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ContentReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val calls = mutableListOf<Pair<ContentReportTarget, String>>()
    private var current = true
    private var response: suspend () -> Result<Unit> = { Result.success(Unit) }
    private val repository = ContentReportRepository { target, reason -> calls += target to reason; response() }
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private fun model(preview: Boolean = false, loggedIn: Boolean = true, access: Boolean = true,
        kind: ContentReportKind = ContentReportKind.CheckIn, id: String = "publication") =
        ContentReportViewModel(ContentReportTarget(kind, id), preview, loggedIn, access, repository, { current })
            .also { store.put("form", it) }
    private fun TestScope.send(model: ContentReportViewModel, action: ContentReportAction) {
        model.onAction(action)
        runCurrent()
    }
    private fun TestScope.type(model: ContentReportViewModel, text: String = "  Причина  ") = send(model, ContentReportAction.TextChanged(text))

    @Test fun blankIsValidatedAndPreviewCompletesWithoutAuthenticationOrRequests() = runTest(dispatcher) {
        val model = model(preview = true, loggedIn = false, id = "")
        type(model, "   ")
        send(model, ContentReportAction.Submit())
        assertEquals(ContentReportProblem.InvalidInput, model.state.value.error?.problem)
        type(model)
        send(model, ContentReportAction.Submit())
        send(model, ContentReportAction.Submit())
        assertTrue(model.state.value.isSubmitted)
        assertEquals("", model.state.value.text)
        assertTrue(calls.isEmpty())
    }

    @Test fun pendingSubmitIsSingleAndFieldsCannotChangeUntilItFinishes() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        response = { gate.await() }
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        send(model, ContentReportAction.Submit())
        type(model, "changed")
        assertTrue(model.state.value.isSubmitting)
        assertEquals("  Причина  ", model.state.value.text)
        assertEquals(listOf(ContentReportTarget(ContentReportKind.CheckIn, "publication") to "Причина"), calls)
        gate.complete(Result.success(Unit))
        runCurrent()
        assertTrue(model.state.value.isSubmitted)
        assertFalse(model.state.value.isSubmitting)
    }

    @Test fun guestGoesToSignInOnceUntilReturningAndDoesNotSend() = runTest(dispatcher) {
        val model = model(loggedIn = false)
        val events = mutableListOf<ContentReportEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.collect { events += it.event } }
        type(model)
        send(model, ContentReportAction.Submit())
        send(model, ContentReportAction.Submit())
        assertEquals(listOf(ContentReportEvent.SignIn), events)
        assertEquals(ContentReportProblem.SignInRequired, model.state.value.error?.problem)
        send(model, ContentReportAction.Resume)
        send(model, ContentReportAction.Submit())
        assertEquals(listOf(ContentReportEvent.SignIn, ContentReportEvent.SignIn), events)
        assertTrue(calls.isEmpty())
    }

    @Test fun reviewTargetAndExplicitBusinessRejectionCanBeCorrectedAndResubmitted() = runTest(dispatcher) {
        response = { Result.failure(ContentReportFailure(ContentReportProblem.Rejected, "Исправьте причину")) }
        val model = model(kind = ContentReportKind.Review)
        type(model)
        send(model, ContentReportAction.Submit())
        assertEquals("Исправьте причину", model.state.value.error?.serverMessage)
        assertFalse(model.state.value.deliveryUnconfirmed)
        response = { Result.success(Unit) }
        type(model, "Другая причина")
        send(model, ContentReportAction.Submit())
        assertTrue(model.state.value.isSubmitted)
        assertTrue(calls.all { it.first.kind == ContentReportKind.Review })
        assertEquals(2, calls.size)
    }

    @Test fun unknownDeliveryBlocksEveryFurtherSubmitAndHidesExceptionDetails() = runTest(dispatcher) {
        response = { throw IllegalStateException("private URL and reason") }
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        assertTrue(model.state.value.deliveryUnconfirmed)
        assertNull(model.state.value.error)
        assertFalse(model.state.value.isSubmitting)
        send(model, ContentReportAction.Submit())
        type(model, "changed")
        assertEquals(1, calls.size)
        assertEquals("  Причина  ", model.state.value.text)
    }

    @Test fun resultCancellationRemainsCancellationAndCannotEnableRetry() = runTest(dispatcher) {
        response = { Result.failure(CancellationException("cancelled")) }
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        assertTrue(model.state.value.deliveryUnconfirmed)
        assertFalse(model.state.value.isSubmitting)
        assertNull(model.state.value.error)
        send(model, ContentReportAction.Submit())
        assertEquals(1, calls.size)
    }

    @Test fun aLateSuccessAfterAccountReplacementCannotMarkTheOldFormSubmitted() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        response = { gate.await() }
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        current = false
        gate.complete(Result.success(Unit))
        runCurrent()
        assertFalse(model.state.value.isSubmitted)
        assertFalse(model.state.value.isSubmitting)
        send(model, ContentReportAction.Submit())
        assertEquals(1, calls.size)
    }

    @Test fun unauthorizedResponseRequestsSignInWithoutClaimingSuccess() = runTest(dispatcher) {
        response = { Result.failure(ContentReportFailure(ContentReportProblem.SignInRequired)) }
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        assertEquals(ContentReportEvent.SignIn, model.events.first().event)
        assertFalse(model.state.value.isSubmitted)
        assertFalse(model.state.value.isSubmitting)
    }

    @Test fun unavailableAccessCanBeRetriedButCannotSend() = runTest(dispatcher) {
        val model = model(access = false)
        type(model)
        send(model, ContentReportAction.Submit())
        send(model, ContentReportAction.RetryAccess())
        assertEquals(ContentReportEvent.RetryAccess, model.events.first().event)
        assertTrue(calls.isEmpty())
        assertEquals("", model.state.value.text)
    }

    @Test fun backWorksDuringSubmissionAndBelongsToItsSurface() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        response = { gate.await() }
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        val owner = Any()
        send(model, ContentReportAction.Back(owner))
        val event = model.events.first()
        assertEquals(ContentReportEvent.Back, event.forOwner(owner))
        assertNull(event.forOwner(Any()))
        gate.complete(Result.success(Unit))
    }

    @Test fun submittedFormCannotBeEditedOrSentAgain() = runTest(dispatcher) {
        val model = model()
        type(model)
        send(model, ContentReportAction.Submit())
        type(model, "changed")
        send(model, ContentReportAction.Submit())
        assertEquals("", model.state.value.text)
        assertEquals(1, calls.size)
    }

    @Test fun missingPublicationFailsBeforeCallingTheRepository() = runTest(dispatcher) {
        val model = model(id = " ")
        type(model)
        send(model, ContentReportAction.Submit())
        assertEquals(ContentReportProblem.NotFound, model.state.value.error?.problem)
        assertTrue(calls.isEmpty())
    }

    @Test fun lengthLimitNeverKeepsHalfOfTheLastEmoji() = runTest(dispatcher) {
        val model = model()
        type(model, "x".repeat(1999) + "☕️😀")
        assertTrue(model.state.value.text.length <= 2000)
        assertEquals("x".repeat(1999), limitReportText("x".repeat(1999) + "😀"))
        assertEquals(2000, limitReportText("x".repeat(2001)).length)
    }
}
