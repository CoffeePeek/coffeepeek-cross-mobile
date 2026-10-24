package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.ProfileDeletionRequest
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionAction
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Port of the legacy `DeleteAccountPendingViewModelTest`; the open side effect flipped with the owner-approved confirmation. */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class AccountDeletionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    private var clock = Instant.parse("2026-10-20T10:00:00Z")
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val past = "2026-10-20T09:00:00Z"
    private val soon = "2026-10-20T10:00:03Z"
    private fun request(at: String) = ProfileDeletionRequest(at)

    private fun model(deletion: FakeDeletion, accounts: FakeProfileAccounts = FakeProfileAccounts()) =
        AccountDeletionViewModel(accounts, deletion, now = { clock }).also { store.put("delete-${nextKey++}", it) }

    /** The resend ticker only ends when the clock passes the server time, so a test that leaves it waiting must close the model first. */
    private fun closeModels() = store.clear()

    private fun AccountDeletionViewModel.act(action: AccountDeletionAction) { onAction(action); dispatcher.scheduler.runCurrent() }

    @Test fun anExistingRequestIsShownWithoutIssuingANewOne() = runTest(dispatcher) {
        val deletion = FakeDeletion(existing = Result.success(request(soon)))
        val vm = model(deletion); runCurrent()
        assertFalse(vm.state.value.isLoading); assertTrue(vm.state.value.requested)
        assertEquals(0, deletion.requests); assertEquals("anna@example.com", vm.state.value.email)
        assertFalse(vm.state.value.resendAvailable)
        closeModels()
    }

    /** Owner decision 2026-10-20: opening must not request deletion; production did. */
    @Test fun openingWithoutARequestOnlyAsksForConfirmation() = runTest(dispatcher) {
        val deletion = FakeDeletion(existing = Result.success(null))
        val vm = model(deletion); runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(0, deletion.requests)
        assertFalse(vm.state.value.requested); assertTrue(vm.state.value.canSend)
    }

    @Test fun theExplicitSendIssuesOneRequestAndGatesTheResend() = runTest(dispatcher) {
        val deletion = FakeDeletion(existing = Result.success(null), created = Result.success(request(soon)))
        val vm = model(deletion); runCurrent()
        vm.act(AccountDeletionAction.Send)
        assertEquals(1, deletion.requests); assertTrue(vm.state.value.requested); assertFalse(vm.state.value.resendAvailable)
        vm.act(AccountDeletionAction.Send)
        assertEquals(1, deletion.requests)
        closeModels()
    }

    @Test fun twoSendsInTheSameFrameIssueOneRequest() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val deletion = FakeDeletion(existing = Result.success(null), created = Result.success(request(soon)), gate = gate)
        val vm = model(deletion); runCurrent()
        vm.onAction(AccountDeletionAction.Send); vm.onAction(AccountDeletionAction.Send); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(1, deletion.requests)
        closeModels()
    }

    @Test fun statusLoadFailureShowsItsMessageOrTheFallbackAndSendsNothing() = runTest(dispatcher) {
        val deletion = FakeDeletion(existing = Result.failure(RuntimeException()))
        val vm = model(deletion); runCurrent()
        assertEquals("Ошибка загрузки статуса", vm.state.value.error); assertEquals(0, deletion.requests)
        val named = model(FakeDeletion(existing = Result.failure(RuntimeException("offline")))); runCurrent()
        assertEquals("offline", named.state.value.error)
    }

    @Test fun aFailedSendShowsTheFallbackAndCanBeRetried() = runTest(dispatcher) {
        val deletion = FakeDeletion(existing = Result.success(null), created = Result.failure(RuntimeException()))
        val vm = model(deletion); runCurrent()
        vm.act(AccountDeletionAction.Send)
        assertEquals("Не удалось отправить письмо", vm.state.value.error)
        assertFalse(vm.state.value.requested); assertTrue(vm.state.value.canSend)
        vm.act(AccountDeletionAction.DismissError); assertEquals(null, vm.state.value.error)
    }

    @Test fun resendIsGatedUntilTheServerTimeAndBlankOrUnparsableMeansAvailable() = runTest(dispatcher) {
        val waiting = model(FakeDeletion(existing = Result.success(request(soon)))); runCurrent()
        assertFalse(waiting.state.value.resendAvailable)
        for (at in listOf(past, "", "not a date", "2026-10-20T09:00:00")) {
            val vm = model(FakeDeletion(existing = Result.success(request(at)))); runCurrent()
            assertTrue(vm.state.value.resendAvailable, "'$at' must allow resend")
        }
        closeModels()
    }

    @Test fun theTickerOpensTheResendWhenTheServerTimePasses() = runTest(dispatcher) {
        val vm = model(FakeDeletion(existing = Result.success(request(soon)))); runCurrent()
        assertFalse(vm.state.value.resendAvailable)
        clock = Instant.parse("2026-10-20T10:00:04Z"); advanceTimeBy(1_000); runCurrent()
        assertTrue(vm.state.value.resendAvailable)
        closeModels()
    }

    @Test fun theEndOfAnActiveSessionIsReportedOnce() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts()
        val vm = model(FakeDeletion(existing = Result.success(null)), accounts); runCurrent()
        val events = mutableListOf<AccountDeletionEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        runCurrent(); assertTrue(events.isEmpty())
        accounts.signIn(null); runCurrent()
        assertEquals(listOf<AccountDeletionEvent>(AccountDeletionEvent.SessionEnded), events)
        accounts.signIn(null); runCurrent()
        assertEquals(1, events.size)
    }

    @Test fun replacingTheAccountByAnotherOneEndsTheFormToo() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts()
        val vm = model(FakeDeletion(existing = Result.success(null)), accounts); runCurrent()
        val events = mutableListOf<AccountDeletionEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        runCurrent(); assertTrue(events.isEmpty())
        accounts.signIn("b"); runCurrent()
        assertEquals(listOf<AccountDeletionEvent>(AccountDeletionEvent.SessionEnded), events)
    }

    @Test fun aGuestSessionNeverReportsAnEnd() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts(user = null)
        val vm = model(FakeDeletion(existing = Result.success(null)), accounts)
        val events = mutableListOf<AccountDeletionEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        runCurrent(); assertTrue(events.isEmpty())
    }

    private class FakeDeletion(
        private val existing: Result<ProfileDeletionRequest?>,
        private val created: Result<ProfileDeletionRequest> = Result.failure(IllegalStateException("none")),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : AccountDeletionPort {
        var requests = 0
        override suspend fun existingRequest() = existing
        override suspend fun request(): Result<ProfileDeletionRequest> { requests++; gate?.await(); return created }
    }
}
