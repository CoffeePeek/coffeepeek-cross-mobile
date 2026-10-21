package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.impl.login.LoginAction
import com.coffeepeek.feature.auth.impl.login.LoginEvent
import com.coffeepeek.feature.auth.impl.login.LoginViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(auth: FakeAuthRepository = FakeAuthRepository(), analytics: FakeAnalytics = FakeAnalytics()) =
        LoginViewModel(auth, analytics).also { store.put("login", it) }

    private fun TestScope.events(vm: LoginViewModel) = mutableListOf<LoginEvent>().also { list -> backgroundScope.launch { vm.events.collect { list += it } } }

    private fun LoginViewModel.type(email: String, password: String) {
        onAction(LoginAction.EmailChanged(email)); onAction(LoginAction.PasswordChanged(password))
    }

    @Test fun emptySubmitMarksBothFieldsAndSendsNothing() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth)
        vm.onAction(LoginAction.Submit); runCurrent()
        assertEquals(EmailIssue.Empty, vm.state.value.emailIssue)
        assertEquals(PasswordIssue.Empty, vm.state.value.passwordIssue)
        assertNull(vm.state.value.failure)
        assertTrue(auth.signIns.isEmpty())
    }

    @Test fun anInvalidEmailAndAShortPasswordAreReportedTogether() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val vm = model(auth)
        vm.type("not-an-email", "123"); vm.onAction(LoginAction.Submit); runCurrent()
        assertEquals(EmailIssue.Invalid, vm.state.value.emailIssue)
        assertEquals(PasswordIssue.TooShort(6), vm.state.value.passwordIssue)
        assertTrue(auth.signIns.isEmpty())
    }

    @Test fun aValidSubmitSendsTheTrimmedEmailAndThePasswordAsTypedThenReportsAndEmitsSignedIn() = runTest(dispatcher) {
        val auth = FakeAuthRepository(); val analytics = FakeAnalytics(); val vm = model(auth, analytics); val events = events(vm)
        vm.type("  Person@Example.com ", " pass 123 "); vm.onAction(LoginAction.Submit); runCurrent()
        assertEquals(listOf("Person@Example.com" to " pass 123 "), auth.signIns)
        assertEquals<List<List<Any?>>>(listOf(listOf("user-1", SignInMethod.Password, AuthEntry.Login, "Person@Example.com")), analytics.signedIn)
        assertEquals(listOf<LoginEvent>(LoginEvent.SignedIn), events)
        assertFalse(vm.state.value.signingIn)
        assertNull(vm.state.value.failure)
    }

    @Test fun aFailureIsClassifiedStaysOnTheScreenAndSendsNoEvent() = runTest(dispatcher) {
        val cases = mapOf(
            "Invalid credentials" to LoginFailure.WrongCredentials,
            "Connection timeout" to LoginFailure.Network,
            "boom" to LoginFailure.Unknown,
        )
        for ((message, expected) in cases) {
            val auth = FakeAuthRepository().apply { signIn = Result.failure(IllegalStateException(message)) }
            val analytics = FakeAnalytics(); val vm = model(auth, analytics); val events = events(vm)
            vm.type("person@example.com", "secret1"); vm.onAction(LoginAction.Submit); runCurrent()
            assertEquals(expected, vm.state.value.failure, message)
            assertFalse(vm.state.value.signingIn)
            assertTrue(events.isEmpty() && analytics.signedIn.isEmpty())
        }
    }

    @Test fun theFormLocksWhileTheRequestRunsAndASecondSubmitIsIgnored() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { gate = CompletableDeferred() }
        val vm = model(auth); val events = events(vm)
        vm.type("person@example.com", "secret1"); vm.onAction(LoginAction.Submit); runCurrent()
        assertTrue(vm.state.value.signingIn)
        vm.onAction(LoginAction.Submit); runCurrent()
        assertEquals(1, auth.signIns.size)
        auth.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.state.value.signingIn)
        assertEquals(listOf<LoginEvent>(LoginEvent.SignedIn), events)
    }

    @Test fun aShownFieldErrorIsValidatedLiveOnlyWhileItIsShownAndEditingClearsTheFailure() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { signIn = Result.failure(IllegalStateException("Invalid credentials")) }
        val vm = model(auth)
        vm.onAction(LoginAction.EmailChanged("bad")); vm.onAction(LoginAction.PasswordChanged("x")); runCurrent()
        assertNull(vm.state.value.emailIssue, "No live validation before the first submit")
        assertNull(vm.state.value.passwordIssue)

        vm.onAction(LoginAction.Submit); runCurrent()
        assertEquals(EmailIssue.Invalid, vm.state.value.emailIssue)
        vm.onAction(LoginAction.EmailChanged("still-bad")); runCurrent()
        assertEquals(EmailIssue.Invalid, vm.state.value.emailIssue, "While shown it is validated live")
        vm.onAction(LoginAction.EmailChanged("good@example.com")); runCurrent()
        assertNull(vm.state.value.emailIssue, "A valid value clears it")
        vm.onAction(LoginAction.EmailChanged("bad-again")); runCurrent()
        assertNull(vm.state.value.emailIssue, "Once cleared, nothing is shown until the next submit")
        vm.onAction(LoginAction.PasswordChanged("secret1")); runCurrent()
        assertNull(vm.state.value.passwordIssue)

        vm.onAction(LoginAction.EmailChanged("good@example.com")); vm.onAction(LoginAction.Submit); runCurrent()
        assertEquals(LoginFailure.WrongCredentials, vm.state.value.failure)
        vm.onAction(LoginAction.PasswordChanged("secret12")); runCurrent()
        assertNull(vm.state.value.failure, "Editing clears the request error")
    }
}
