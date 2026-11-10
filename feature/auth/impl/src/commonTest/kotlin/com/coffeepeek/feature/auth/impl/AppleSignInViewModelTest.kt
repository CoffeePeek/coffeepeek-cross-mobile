package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.api.*
import com.coffeepeek.feature.auth.domain.SocialProvider
import com.coffeepeek.feature.auth.impl.login.*
import com.coffeepeek.feature.auth.impl.register.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppleSignInViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeAuthRepository()
    private val analytics = FakeAnalytics()
    private val authorizer = Authorizer()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun login() = LoginViewModel(repository, analytics, authorizer).also { store.put("login", it) }
    private fun register() = RegisterViewModel(repository, analytics, authorizer).also { store.put("register", it) }

    @Test fun loginUsesOneServerChallengeAndReportsAppleExactlyOnce() = runTest(dispatcher) {
        val vm = login()
        val events = mutableListOf<LoginEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.onAction(LoginAction.AppleSignIn)
        runCurrent()
        assertEquals(listOf("nonce" to "challenge"), authorizer.requests)
        assertEquals(listOf("challenge" to "identity"), repository.appleTokens)
        assertEquals<List<List<Any?>>>(listOf(listOf("apple-user", SignInMethod.Apple, AuthEntry.Login, null)), analytics.signedIn)
        assertEquals(listOf<LoginEvent>(LoginEvent.SignedIn), events)
        assertFalse(vm.state.value.signingIn)
    }

    @Test fun registerUsesTheSameFlowWithoutPasswordRegistrationOrConsentWrites() = runTest(dispatcher) {
        val vm = register()
        val events = mutableListOf<RegisterEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.onAction(RegisterAction.AppleSignIn)
        runCurrent()
        assertEquals<List<List<Any?>>>(listOf(listOf("apple-user", SignInMethod.Apple, AuthEntry.Register, null)), analytics.signedIn)
        assertEquals(listOf<RegisterEvent>(RegisterEvent.SignedIn), events)
        assertTrue(repository.registrations.isEmpty())
        assertTrue(analytics.consents.isEmpty())
        assertFalse(vm.state.value.busy)
    }

    @Test fun cancellationIsSilentOnBothScreensAndDoesNotExchangeTheToken() = runTest(dispatcher) {
        authorizer.failure = CancellationException("native cancellation")
        val login = login()
        val register = register()
        login.onAction(LoginAction.AppleSignIn)
        register.onAction(RegisterAction.AppleSignIn)
        runCurrent()
        assertNull(login.state.value.socialFailure)
        assertNull(register.state.value.socialFailure)
        assertFalse(login.state.value.signingIn)
        assertFalse(register.state.value.busy)
        assertTrue(repository.appleTokens.isEmpty())
        assertTrue(analytics.signedIn.isEmpty())
    }

    @Test fun aChallengeFailureNeverOpensTheNativeDialog() = runTest(dispatcher) {
        repository.challenge = Result.failure(IllegalStateException("offline"))
        val vm = login()
        vm.onAction(LoginAction.AppleSignIn)
        runCurrent()
        assertTrue(authorizer.requests.isEmpty())
        assertTrue(repository.appleTokens.isEmpty())
        assertEquals("ASI-CH", vm.state.value.socialFailure?.diagnostic?.code)
        assertEquals(SocialProvider.Apple, vm.state.value.socialFailure?.provider)
        assertTrue(analytics.signedIn.isEmpty())
    }

    @Test fun aServerFailureKeepsAppleDiagnosticsAndNeverNavigates() = runTest(dispatcher) {
        repository.apple = Result.failure(IllegalStateException("Rejected [ASI-H403]"))
        val vm = register()
        vm.onAction(RegisterAction.AppleSignIn)
        runCurrent()
        assertEquals("ASI-H403", vm.state.value.socialFailure?.diagnostic?.code)
        assertEquals(SocialProvider.Apple, vm.state.value.socialFailure?.provider)
        assertFalse(vm.state.value.busy)
        assertTrue(analytics.signedIn.isEmpty())
    }

    @Test fun nativeAuthorizationBlocksDuplicateAndCompetingLoginRequests() = runTest(dispatcher) {
        authorizer.gate = CompletableDeferred()
        val vm = login()
        vm.onAction(LoginAction.AppleSignIn)
        runCurrent()
        vm.onAction(LoginAction.AppleSignIn)
        vm.onAction(LoginAction.Submit)
        vm.onAction(LoginAction.GoogleToken("competing"))
        runCurrent()
        assertEquals(1, repository.challenges)
        assertTrue(vm.state.value.signingIn)
        assertTrue(repository.googleTokens.isEmpty())
        authorizer.gate!!.complete(Unit)
        runCurrent()
        assertEquals(1, repository.appleTokens.size)
        assertEquals(1, analytics.signedIn.size)
    }

    @Test fun retryAfterCancellationAsksForANewChallenge() = runTest(dispatcher) {
        val vm = login()
        authorizer.failure = CancellationException("cancel")
        vm.onAction(LoginAction.AppleSignIn)
        runCurrent()
        authorizer.failure = null
        vm.onAction(LoginAction.AppleSignIn)
        runCurrent()
        assertEquals(2, repository.challenges)
        assertEquals(1, repository.appleTokens.size)
        assertEquals(1, analytics.signedIn.size)
    }

    @Test fun registrationIgnoresCompetingProvidersAndEditsWhileAppleIsOpen() = runTest(dispatcher) {
        authorizer.gate = CompletableDeferred()
        val vm = register()
        vm.onAction(RegisterAction.AppleSignIn)
        runCurrent()
        vm.onAction(RegisterAction.GoogleToken("competing"))
        vm.onAction(RegisterAction.GoogleFailed("wrong provider"))
        vm.onAction(RegisterAction.EmailChanged("changed@example.com"))
        vm.onAction(RegisterAction.AppleSignIn)
        runCurrent()
        assertEquals(1, repository.challenges)
        assertTrue(repository.googleTokens.isEmpty())
        assertEquals("", vm.state.value.email)
        assertNull(vm.state.value.socialFailure)
        authorizer.gate!!.complete(Unit)
        runCurrent()
        assertEquals(1, analytics.signedIn.size)
    }

    @Test fun absentNativeProviderCannotStartApple() = runTest(dispatcher) {
        val vm = LoginViewModel(repository, analytics).also { store.put("login", it) }
        vm.onAction(LoginAction.AppleSignIn)
        runCurrent()
        assertEquals(0, repository.challenges)
        assertFalse(vm.state.value.signingIn)
    }

    private class Authorizer : AppleAuthorizer {
        val requests = mutableListOf<Pair<String, String>>()
        var failure: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun authorize(nonce: String, state: String): String {
            requests += nonce to state
            gate?.await()
            failure?.let { throw it }
            return "identity"
        }
    }
}
