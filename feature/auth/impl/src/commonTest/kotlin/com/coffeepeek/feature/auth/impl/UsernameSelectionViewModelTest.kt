package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.api.*
import com.coffeepeek.feature.auth.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameSelectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val account = Accounts()
    private val backend = Backend()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { store.clear(); Dispatchers.resetMain() }
    private fun model() = UsernameSelectionViewModel(account, backend) {
        account.owner.value = null
        Result.success(Unit)
    }.also { store.put("setup", it) }

    @Test fun unchangedNameCanBeConfirmedAndDuplicateSubmissionIsGuarded() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        assertEquals(UsernameSelectionPhase.Required, model.state.value.phase)
        backend.saveGate = CompletableDeferred()
        model.onAction(UsernameSelectionAction.Submit)
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        assertEquals(1, backend.saves)
        assertTrue(model.state.value.blocksApp)
        backend.saveGate!!.complete(Unit)
        runCurrent()
        assertTrue(model.state.value.readyFor("A", 1))
    }

    @Test fun failedProfileReadCannotOpenTheAppAndRetryRecovers() = runTest(dispatcher) {
        backend.profile = Result.failure(Exception("offline"))
        val model = model()
        runCurrent()
        assertEquals(UsernameSelectionPhase.Failed, model.state.value.phase)
        assertTrue(model.state.value.blocksApp)
        backend.profile = Result.success(UsernameSelectionProfile("chosen", false))
        model.onAction(UsernameSelectionAction.Retry)
        runCurrent()
        assertTrue(model.state.value.readyFor("A", 1))
    }

    @Test fun invalidAndTakenNamesKeepTheFormAndInput() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(UsernameSelectionAction.Edit("1!"))
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        assertEquals(0, backend.saves)
        assertEquals(UsernameSelectionError.Invalid, model.state.value.error)
        backend.saveResult = Result.failure(UsernameSelectionException(UsernameSelectionFailure.Taken))
        model.onAction(UsernameSelectionAction.Edit("coffee"))
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        assertEquals("coffee", model.state.value.username)
        assertEquals(UsernameSelectionError.Taken, model.state.value.error)
    }

    @Test fun lateSaveFromAnEarlierLoginCannotReleaseTheSameAccountAfterRelogin() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        backend.saveGate = CompletableDeferred()
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        account.owner.value = UsernameSelectionOwner("B", 2)
        runCurrent()
        account.owner.value = UsernameSelectionOwner("A", 3)
        runCurrent()
        backend.saveGate!!.complete(Unit)
        runCurrent()
        assertEquals(UsernameSelectionPhase.Required, model.state.value.phase)
        assertEquals(3L, model.state.value.accountGeneration)
    }

    @Test fun confirmationFromAnotherDeviceIsRecoveredByAnAuthoritativeRead() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        backend.saveResult = Result.failure(UsernameSelectionException(UsernameSelectionFailure.Conflict))
        backend.profile = Result.success(UsernameSelectionProfile("other_device", false))
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        assertEquals(UsernameSelectionPhase.Ready, model.state.value.phase)
        assertEquals("other_device", model.state.value.username)
    }

    @Test fun signOutLeavesSetupWithoutReleasingAnAuthenticatedAccount() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(UsernameSelectionAction.SignOut)
        runCurrent()
        assertEquals(UsernameSelectionPhase.SignedOut, model.state.value.phase)
        assertNull(model.state.value.accountId)
    }

    @Test fun secureStorageObservationFailureCanBeRetriedWithoutRecreatingTheApp() = runTest(dispatcher) {
        account.failNextObservation = true
        val model = model()
        runCurrent()
        assertEquals(UsernameSelectionPhase.Failed, model.state.value.phase)
        model.onAction(UsernameSelectionAction.Retry)
        runCurrent()
        assertEquals(UsernameSelectionPhase.Required, model.state.value.phase)
    }

    @Test fun lostSaveResponseIsReconciledByReadWithoutAnotherPut() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        backend.saveResult = Result.failure(Exception("response lost"))
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        assertEquals(UsernameSelectionPhase.Failed, model.state.value.phase)
        backend.profile = Result.success(UsernameSelectionProfile("chosen", false))
        model.onAction(UsernameSelectionAction.Retry)
        runCurrent()
        assertEquals(1, backend.saves)
        assertEquals(UsernameSelectionPhase.Ready, model.state.value.phase)
    }

    @Test fun explicitExitDuringSaveCannotBeUndoneByItsLateResponse() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        backend.saveGate = CompletableDeferred()
        model.onAction(UsernameSelectionAction.Submit)
        runCurrent()
        model.onAction(UsernameSelectionAction.SignOut)
        runCurrent()
        backend.saveGate!!.complete(Unit)
        runCurrent()
        assertEquals(UsernameSelectionPhase.SignedOut, model.state.value.phase)
    }

    private class Accounts : UsernameSelectionAccount {
        val owner = MutableStateFlow<UsernameSelectionOwner?>(UsernameSelectionOwner("A", 1))
        var failNextObservation = false
        override fun current() = owner.value
        override fun observe() = flow {
            if (failNextObservation) { failNextObservation = false; throw IllegalStateException("storage unavailable") }
            emitAll(owner)
        }
    }

    private class Backend : UsernameSelectionRepository {
        var profile = Result.success(UsernameSelectionProfile("generated", true))
        var saveResult = Result.success(UsernameSelectionProfile("chosen", false))
        var saveGate: CompletableDeferred<Unit>? = null
        var saves = 0
        override suspend fun load(owner: UsernameSelectionOwner) = profile
        override suspend fun complete(owner: UsernameSelectionOwner, username: String): Result<UsernameSelectionProfile> {
            saves++
            saveGate?.await()
            return saveResult
        }
    }
}
