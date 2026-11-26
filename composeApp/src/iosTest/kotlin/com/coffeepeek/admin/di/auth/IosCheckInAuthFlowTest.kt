package com.coffeepeek.admin.di.auth

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.ShopCheckInSignInHandoff
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.AppleSignInChallenge
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.SignedInAccount
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class IosCheckInAuthFlowTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun switchingFormsKeepsTheirDraftsUntilTheCheckInRouteCloses() = runTest(dispatcher) {
        val fixture = Fixture()
        val login = fixture.login()
        login.emailChanged("guest@example.com")
        login.register()
        val registration = assertIs<IosRegister>(fixture.flow.currentScreen())
        registration.nameChanged("Guest Name")
        registration.login()
        runCurrent()
        assertSame(login, fixture.flow.currentScreen())
        assertEquals("guest@example.com", login.currentState().email)
        login.register()
        assertSame(registration, fixture.flow.currentScreen())
        assertEquals("Guest Name", registration.currentState().name)
        registration.back()
        fixture.flow.close()
        assertEquals(1, fixture.pops)
        assertNull(fixture.handoff.take())
    }

    @Test
    fun recoveryPrefillsTheEmailAndReturnsToTheSameCheckInLogin() = runTest(dispatcher) {
        val fixture = Fixture()
        val login = fixture.login()
        login.forgotPassword("guest@example.com")
        val recovery = assertIs<IosPasswordRecovery>(fixture.flow.currentScreen())
        assertEquals("guest@example.com", recovery.currentState().email)
        recovery.back()
        assertSame(login, fixture.flow.currentScreen())
        login.forgotPassword("guest@example.com")
        assertSame(recovery, fixture.flow.currentScreen())
        recovery.login()
        assertSame(login, fixture.flow.currentScreen())
        login.forgotPassword("another@example.com")
        assertEquals("another@example.com", assertIs<IosPasswordRecovery>(fixture.flow.currentScreen()).currentState().email)
        fixture.flow.close()
        assertEquals(0, fixture.pops)
        assertNull(fixture.handoff.take())
    }

    @Test
    fun sessionActivationResumesTheOriginalShopWithoutPoppingToOrdinaryLogin() = runTest(dispatcher) {
        val fixture = Fixture()
        fixture.login().apply {
            emailChanged("guest@example.com")
            passwordChanged("password123")
            submit()
        }
        runCurrent()
        fixture.flow.close()
        assertEquals(0, fixture.pops)
        assertEquals(Navigator.Screen.ShopCheckInEditor("original-shop"), fixture.handoff.take())
        assertNull(fixture.handoff.take())
    }

    @Test
    fun leavingGuestSignInCancelsThePendingRequestAndDoesNotOpenTheEditor() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val fixture = Fixture(gate)
        fixture.login().apply {
            emailChanged("guest@example.com")
            passwordChanged("password123")
            submit()
        }
        runCurrent()
        fixture.flow.close()
        gate.complete(Unit)
        runCurrent()
        assertFalse(fixture.repository.activeSession)
        assertNull(fixture.handoff.take())
    }

    private class Fixture(gate: CompletableDeferred<Unit>? = null) {
        val repository = Repository(gate)
        val handoff = ShopCheckInSignInHandoff()
        var pops = 0
        val flow = IosAuthDestinations(repository, Analytics, Language, apple = null, googleAvailable = false)
            .shopCheckInSignIn(handoff.attempt("original-shop") { repository.activeSession }) { pops++ }

        fun login() = assertIs<IosLogin>(flow.currentScreen())
    }

    private class Repository(private val gate: CompletableDeferred<Unit>?) : AuthRepository {
        var activeSession = false
            private set

        override suspend fun signIn(email: String, password: String): Result<SignedInAccount> {
            gate?.await()
            activeSession = true
            return Result.success(SignedInAccount("test-user"))
        }

        override suspend fun signInWithGoogle(idToken: String, analyticsEnabled: Boolean?): Result<SignedInAccount> =
            error("Google is not part of this test")

        override suspend fun beginAppleSignIn(): Result<AppleSignInChallenge> = error("Apple is not part of this test")

        override suspend fun signInWithApple(challengeId: String, idToken: String, analyticsEnabled: Boolean?): Result<SignedInAccount> =
            error("Apple is not part of this test")

        override suspend fun register(userName: String, email: String, password: String, analyticsEnabled: Boolean) =
            error("Registration is not part of this test")

        override suspend fun isEmailTaken(email: String) = Result.success(false)
        override suspend fun requestPasswordReset(email: String) = Result.success(Unit)
        override suspend fun signOut() = error("Sign-out is not part of this test")
    }

    private object Analytics : AuthAnalytics {
        override fun signedIn(userId: String?, method: SignInMethod, entry: AuthEntry, email: String?) = Unit
        override suspend fun saveAnalyticsConsent(accepted: Boolean) = Unit
        override fun passwordSignUpCompleted(email: String) = Unit
    }

    private object Language : LanguagePort {
        override fun current() = AppLanguage.RUSSIAN
        override fun observe() = flowOf(current())
        override suspend fun set(language: AppLanguage) = error("Language changes are not part of this test")
    }
}
