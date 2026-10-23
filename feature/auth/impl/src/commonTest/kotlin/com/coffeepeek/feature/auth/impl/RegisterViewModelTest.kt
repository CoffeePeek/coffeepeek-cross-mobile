package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.domain.RegistrationIssue
import com.coffeepeek.feature.auth.impl.register.RegisterAction
import com.coffeepeek.feature.auth.impl.register.RegisterStep
import com.coffeepeek.feature.auth.impl.register.RegisterViewModel
import com.coffeepeek.feature.auth.impl.register.RequestFailure
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
class RegisterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(auth: FakeAuthRepository = FakeAuthRepository(), analytics: FakeAnalytics = FakeAnalytics()) =
        RegisterViewModel(auth, analytics).also { store.put("register", it) }

    private fun RegisterViewModel.toDetails(email: String = "new@example.com") {
        onAction(RegisterAction.EmailChanged(email)); onAction(RegisterAction.ContinueWithEmail)
    }

    private fun RegisterViewModel.fill(name: String = "Анна", password: String = "password1", terms: Boolean = true) {
        onAction(RegisterAction.NameChanged(name)); onAction(RegisterAction.PasswordChanged(password)); onAction(RegisterAction.TermsChanged(terms))
    }

    @Test fun anEmptyOrInvalidEmailStopsAtTheFirstStepWithoutAskingTheServer() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth)
        vm.onAction(RegisterAction.ContinueWithEmail); runCurrent()
        assertEquals(EmailIssue.Empty, vm.state.value.emailIssue)
        vm.onAction(RegisterAction.EmailChanged("nope")); vm.onAction(RegisterAction.ContinueWithEmail); runCurrent()
        assertEquals(EmailIssue.Invalid, vm.state.value.emailIssue)
        assertTrue(auth.emailChecks.isEmpty())
        assertEquals(RegisterStep.Email, vm.state.value.step)
    }

    @Test fun aFreeEmailMovesToTheDetailsStepWithTheTrimmedAddressChecked() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth)
        vm.toDetails("  new@example.com "); runCurrent()
        assertEquals(listOf("new@example.com"), auth.emailChecks)
        assertEquals(RegisterStep.Details, vm.state.value.step)
        assertFalse(vm.state.value.busy)
    }

    @Test fun aTakenEmailStaysOnTheFirstStepAndEditingClearsTheMark() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { emailTaken = Result.success(true) }; val vm = model(auth)
        vm.toDetails(); runCurrent()
        assertEquals(RegisterStep.Email, vm.state.value.step)
        assertTrue(vm.state.value.emailTaken)
        vm.onAction(RegisterAction.EmailChanged("other@example.com")); runCurrent()
        assertFalse(vm.state.value.emailTaken)
    }

    @Test fun aFailedEmailCheckKeepsTheStepAndShowsTheServerMessage() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { emailTaken = Result.failure(IllegalStateException("Service down")) }; val vm = model(auth)
        vm.toDetails(); runCurrent()
        assertEquals(RegisterStep.Email, vm.state.value.step)
        assertEquals(RequestFailure("Service down"), vm.state.value.failure)
        assertFalse(vm.state.value.busy)
    }

    @Test fun changingTheEmailGoesBackAndDropsThePasswordError() = runTest(dispatcher) {
        val vm = model(); vm.toDetails(); runCurrent()
        vm.onAction(RegisterAction.PasswordChanged("short")); vm.onAction(RegisterAction.TermsChanged(true)); vm.onAction(RegisterAction.NameChanged("Анна"))
        vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(PasswordIssue.TooShort(8), vm.state.value.passwordIssue)
        vm.onAction(RegisterAction.ChangeEmail); runCurrent()
        assertEquals(RegisterStep.Email, vm.state.value.step)
        assertNull(vm.state.value.passwordIssue)
    }

    @Test fun aValidSubmitStoresTheConsentFirstThenRegistersThenRemembersTheSignUp() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val analytics = FakeAnalytics()
        auth.onRegister = { analytics.calls += "register" }
        val vm = model(auth, analytics); vm.toDetails("  new@example.com "); runCurrent()
        vm.fill(name = "  Анна ", password = " pass word1 "); vm.onAction(RegisterAction.AnalyticsChanged(true))
        vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(listOf(listOf("Анна", "new@example.com", " pass word1 ")), auth.registrations)
        assertEquals(listOf("consent", "register", "signup"), analytics.calls)
        assertEquals(listOf(true), analytics.consents)
        assertEquals(listOf("new@example.com"), analytics.signUps)
        assertEquals(RegisterStep.Success, vm.state.value.step)
    }

    @Test fun declinedAnalyticsIsStoredAsDeclined() = runTest(dispatcher) {
        val analytics = FakeAnalytics(); val vm = model(analytics = analytics); vm.toDetails(); runCurrent()
        vm.fill(); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(listOf(false), analytics.consents)
    }

    @Test fun aRejectedRegistrationShowsTheServerMessageAndSkipsTheSignUpMemory() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { register = Result.failure(IllegalStateException("Registration rejected")) }
        val analytics = FakeAnalytics(); val vm = model(auth, analytics); vm.toDetails(); runCurrent()
        vm.fill(); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(RegisterStep.Details, vm.state.value.step)
        assertEquals(RequestFailure("Registration rejected"), vm.state.value.failure)
        assertTrue(analytics.signUps.isEmpty())
        assertFalse(vm.state.value.busy)
        vm.onAction(RegisterAction.NameChanged("Анна К")); runCurrent()
        assertNull(vm.state.value.failure)
    }

    @Test fun aFailureWithoutAMessageIsGeneric() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { register = Result.failure(IllegalStateException()) }
        val vm = model(auth); vm.toDetails(); runCurrent()
        vm.fill(); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(RequestFailure(null), vm.state.value.failure)
    }

    @Test fun anInvalidSubmitReportsOneMessageByTheLegacyPriorityAndSendsNothing() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val analytics = FakeAnalytics(); val vm = model(auth, analytics)
        vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(RegistrationIssue.NameRequired, vm.state.value.reported)
        vm.onAction(RegisterAction.NameChanged("Анна")); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(RegistrationIssue.EmailRequired, vm.state.value.reported)
        vm.onAction(RegisterAction.EmailChanged("a@b.co")); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(RegistrationIssue.PasswordTooShort, vm.state.value.reported)
        vm.onAction(RegisterAction.PasswordChanged("password1")); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(RegistrationIssue.TermsRequired, vm.state.value.reported)
        assertTrue(auth.registrations.isEmpty())
        assertTrue(analytics.consents.isEmpty())
    }

    @Test fun aSecondSubmitWhileBusyIsIgnored() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth); vm.toDetails(); runCurrent()
        vm.fill(); auth.gate = CompletableDeferred()
        vm.onAction(RegisterAction.Submit); runCurrent()
        assertTrue(vm.state.value.busy)
        vm.onAction(RegisterAction.Submit); runCurrent()
        auth.gate!!.complete(Unit); runCurrent()
        assertEquals(1, auth.registrations.size)
        assertFalse(vm.state.value.busy)
    }

    @Test fun theShownPasswordErrorIsValidatedLiveWhileTyping() = runTest(dispatcher) {
        val vm = model(); vm.toDetails(); runCurrent()
        vm.fill(password = "short"); vm.onAction(RegisterAction.Submit); runCurrent()
        assertEquals(PasswordIssue.TooShort(8), vm.state.value.passwordIssue)
        vm.onAction(RegisterAction.PasswordChanged("longenough1")); runCurrent()
        assertNull(vm.state.value.passwordIssue)
    }
}
