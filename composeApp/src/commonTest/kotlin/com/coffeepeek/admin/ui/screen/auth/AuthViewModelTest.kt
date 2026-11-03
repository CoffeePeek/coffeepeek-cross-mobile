package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.admin.auth.russianSocialErrorCopy

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.LoadingHandler
import com.coffeepeek.admin.utils.InputValidationIssue
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The legacy login view model as it behaves today: validation, error mapping, navigation and what it sends. */
class AuthViewModelTest {
    private class FakeAuth : AuthRepository {
        val logins = mutableListOf<Pair<String, String>>()
        val googleTokens = mutableListOf<String>()
        var onLogin: (String, String) -> Result<Session> = { _, _ -> Result.success(Session("token", userId = "user")) }
        var onGoogle: (String) -> Result<Session> = { Result.success(Session("token", userId = "user")) }
        override suspend fun login(email: String, password: String): Result<Session> { logins += email to password; return onLogin(email, password) }
        override suspend fun googleLogin(idToken: String): Result<Session> { googleTokens += idToken; return onGoogle(idToken) }
        override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> = error("Unused")
        override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> = error("Unused")
        override suspend fun register(userName: String, email: String, password: String): Result<Unit> = error("Unused")
        override suspend fun isEmailTaken(email: String): Result<Boolean> = error("Unused")
        override suspend fun logout(): Result<Unit> = error("Unused")
    }

    private fun kotlinx.coroutines.CoroutineScope.mainNavigation(): Deferred<Navigator.NavEvent> = async(start = CoroutineStart.UNDISPATCHED) {
        Navigator.navigationEvents.first { it == Navigator.NavEvent.NavigateTo(Navigator.Screen.Main) }
    }

    private suspend fun idle() = withTimeout(5_000) { LoadingHandler.isLoading.first { !it } }

    @Test fun emptySubmitReportsBothRequiredErrorsAndSendsNothing() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = AuthViewModel(auth)
        try {
            vm.onLoginClick()
            assertEquals(InputValidationIssue.EmailRequired, vm.emailError.value)
            assertEquals(InputValidationIssue.PasswordRequired, vm.passwordError.value)
            assertNull(vm.loginError.value)
            assertTrue(auth.logins.isEmpty())
        } finally { vm.close() }
    }

    @Test fun invalidEmailAndShortPasswordAreReportedTogether() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = AuthViewModel(auth)
        try {
            vm.onEmailChange("not-an-email")
            vm.onPasswordChange("123")
            vm.onLoginClick()
            assertEquals(InputValidationIssue.EmailInvalid, vm.emailError.value)
            assertEquals(InputValidationIssue.PasswordTooShort(6), vm.passwordError.value)
            assertTrue(auth.logins.isEmpty())
        } finally { vm.close() }
    }

    @Test fun successfulLoginTrimsTheEmailKeepsThePasswordAndOpensMain() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = AuthViewModel(auth)
        try {
            val navigation = mainNavigation()
            vm.onEmailChange("  Person@Example.com ")
            vm.onPasswordChange(" pass 123 ")
            vm.onLoginClick()
            withTimeout(5_000) { navigation.await() }
            idle()
            assertEquals(listOf("Person@Example.com" to " pass 123 "), auth.logins)
            assertNull(vm.loginError.value)
            assertNull(vm.emailError.value)
            assertNull(vm.passwordError.value)
        } finally { vm.close() }
    }

    @Test fun failedLoginShowsTheLocalizedMessageAndStaysOnTheScreen() = runBlocking<Unit> {
        val cases = mapOf(
            "Invalid credentials" to LoginFailure.WrongCredentials,
            "Connection timeout" to LoginFailure.Network,
            "boom" to LoginFailure.Unknown,
        )
        for ((message, expected) in cases) {
            val auth = FakeAuth().apply { onLogin = { _, _ -> Result.failure(IllegalStateException(message)) } }
            val vm = AuthViewModel(auth)
            try {
                val navigation = mainNavigation()
                vm.onEmailChange("person@example.com")
                vm.onPasswordChange("secret1")
                vm.onLoginClick()
                withTimeout(5_000) { vm.loginFailure.first { it != null } }
                idle()
                assertEquals(expected, vm.loginFailure.value, message)
                assertFalse(navigation.isCompleted, "No navigation after a failed login")
                navigation.cancel()
            } finally { vm.close() }
        }
    }

    @Test fun editingClearsTheLoginErrorAndFieldErrorsRevalidateOnlyAfterTheyWereShown() = runBlocking<Unit> {
        val auth = FakeAuth().apply { onLogin = { _, _ -> Result.failure(IllegalStateException("Invalid credentials")) } }
        val vm = AuthViewModel(auth)
        try {
            vm.onEmailChange("bad")
            assertNull(vm.emailError.value, "No live validation before the first submit")
            vm.onPasswordChange("x")
            assertNull(vm.passwordError.value)

            vm.onLoginClick()
            assertEquals(InputValidationIssue.EmailInvalid, vm.emailError.value)
            vm.onEmailChange("still-bad")
            assertEquals(InputValidationIssue.EmailInvalid, vm.emailError.value, "While an error is shown it is validated live")
            vm.onEmailChange("good@example.com")
            assertNull(vm.emailError.value, "A valid value clears the shown error")
            vm.onEmailChange("bad-again")
            assertNull(vm.emailError.value, "Once cleared, nothing is shown until the next submit")
            vm.onPasswordChange("secret1")
            assertNull(vm.passwordError.value)

            vm.onEmailChange("good@example.com")
            vm.onLoginClick()
            withTimeout(5_000) { vm.loginFailure.first { it != null } }
            idle()
            vm.onPasswordChange("secret12")
            assertNull(vm.loginFailure.value, "Editing clears the request error")
        } finally { vm.close() }
    }

    @Test fun googleLoginOpensMainAndItsFailuresUseTheSocialMessages() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = AuthViewModel(auth)
        try {
            val navigation = mainNavigation()
            vm.onGoogleLogin("google-token") { russianSocialErrorCopy }
            withTimeout(5_000) { navigation.await() }
            idle()
            assertEquals(listOf("google-token"), auth.googleTokens)
        } finally { vm.close() }

        val failing = FakeAuth().apply { onGoogle = { Result.failure(IllegalStateException("Account with this email already exists")) } }
        val second = AuthViewModel(failing)
        try {
            second.onGoogleLogin("token") { russianSocialErrorCopy }
            withTimeout(5_000) { second.loginError.first { it != null } }
            idle()
            assertEquals("Аккаунт с этим email уже существует. Используйте способ входа, с которым вы регистрировались.", second.loginError.value)
            second.onGoogleLoginError("anything", copy = russianSocialErrorCopy)
            assertEquals("Не удалось войти через Google. Попробуйте ещё раз.", second.loginError.value)
            second.clearFieldErrors()
            assertNull(second.loginError.value)
        } finally { second.close() }
    }
}
