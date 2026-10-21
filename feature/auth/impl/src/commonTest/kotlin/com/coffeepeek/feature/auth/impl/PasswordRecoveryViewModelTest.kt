package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryAction
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryViewModel
import com.coffeepeek.feature.auth.impl.recovery.RecoveryError
import com.coffeepeek.feature.auth.impl.recovery.RecoveryStep
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordRecoveryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(auth: FakeAuthRepository = FakeAuthRepository(), email: String = "") =
        PasswordRecoveryViewModel(email, auth).also { store.put("recovery", it) }

    @Test fun theLoginEmailIsCarriedOver() = runTest(dispatcher) {
        assertEquals("person@example.com", model(email = "person@example.com").state.value.email)
    }

    @Test fun successEndsAtEmailSentUntilAnotherEmailIsRequested() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth)
        vm.onAction(PasswordRecoveryAction.EmailChanged(" person@example.com ")); vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertEquals(listOf(" person@example.com "), auth.resets, "The repository trims and lower-cases")
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
        assertFalse(vm.state.value.busy)
        vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertEquals(1, auth.resets.size, "Nothing is sent from the sent step")
        vm.onAction(PasswordRecoveryAction.RequestAnotherEmail); runCurrent()
        assertEquals(RecoveryStep.Email, vm.state.value.step)
    }

    @Test fun anInvalidOrEmptyEmailDoesNotRequestAReset() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth)
        vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertEquals(RecoveryError.EmailEmpty, vm.state.value.error)
        vm.onAction(PasswordRecoveryAction.EmailChanged("nope")); runCurrent()
        assertNull(vm.state.value.error, "Editing clears the error")
        vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertEquals(RecoveryError.EmailInvalid, vm.state.value.error)
        assertTrue(auth.resets.isEmpty())
    }

    @Test fun aFailedRequestShowsTheErrorAndCanBeRetried() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { reset = Result.failure(IllegalStateException("offline")) }
        val vm = model(auth, "person@example.com")
        vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertEquals(RecoveryError.SendFailed, vm.state.value.error)
        assertEquals(RecoveryStep.Email, vm.state.value.step)
        assertFalse(vm.state.value.busy)
        auth.reset = Result.success(Unit)
        vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
        assertNull(vm.state.value.error)
        assertEquals(2, auth.resets.size)
    }

    @Test fun aPendingRequestBlocksADoubleSubmitAndEdits() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { gate = CompletableDeferred() }
        val vm = model(auth, "person@example.com")
        vm.onAction(PasswordRecoveryAction.Submit); runCurrent()
        assertTrue(vm.state.value.busy)
        vm.onAction(PasswordRecoveryAction.Submit); vm.onAction(PasswordRecoveryAction.EmailChanged("other@example.com")); runCurrent()
        assertEquals(1, auth.resets.size)
        assertEquals("person@example.com", vm.state.value.email, "Edits are ignored while busy")
        auth.gate!!.complete(Unit); runCurrent()
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
    }
}
