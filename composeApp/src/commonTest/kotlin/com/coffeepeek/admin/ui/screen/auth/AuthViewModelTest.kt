package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.LoadingHandler
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
            assertEquals("Введите email", vm.emailError.value)
            assertEquals("Введите пароль", vm.passwordError.value)
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
            assertEquals("Введите корректный email", vm.emailError.value)
            assertEquals("Пароль должен содержать как минимум 6 символов", vm.passwordError.value)
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
            "Invalid credentials" to "Неверный email или пароль",
            "Connection timeout" to "Не удалось подключиться. Проверьте интернет-соединение",
            "boom" to "Не удалось войти. Проверьте email и пароль",
        )
        for ((message, expected) in cases) {
            val auth = FakeAuth().apply { onLogin = { _, _ -> Result.failure(IllegalStateException(message)) } }
            val vm = AuthViewModel(auth)
            try {
                val navigation = mainNavigation()
                vm.onEmailChange("person@example.com")
                vm.onPasswordChange("secret1")
                vm.onLoginClick()
                withTimeout(5_000) { vm.loginError.first { it != null } }
                idle()
                assertEquals(expected, vm.loginError.value, message)
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
            assertEquals("Введите корректный email", vm.emailError.value)
            vm.onEmailChange("still-bad")
            assertEquals("Введите корректный email", vm.emailError.value, "While an error is shown it is validated live")
            vm.onEmailChange("good@example.com")
            assertNull(vm.emailError.value, "A valid value clears the shown error")
            vm.onEmailChange("bad-again")
            assertNull(vm.emailError.value, "Once cleared, nothing is shown until the next submit")
            vm.onPasswordChange("secret1")
            assertNull(vm.passwordError.value)

            vm.onEmailChange("good@example.com")
            vm.onLoginClick()
            withTimeout(5_000) { vm.loginError.first { it != null } }
            idle()
            vm.onPasswordChange("secret12")
            assertNull(vm.loginError.value, "Editing clears the request error")
        } finally { vm.close() }
    }

    @Test fun googleLoginOpensMainAndItsFailuresUseTheSocialMessages() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = AuthViewModel(auth)
        try {
            val navigation = mainNavigation()
            vm.onGoogleLogin("google-token")
            withTimeout(5_000) { navigation.await() }
            idle()
            assertEquals(listOf("google-token"), auth.googleTokens)
        } finally { vm.close() }

        val failing = FakeAuth().apply { onGoogle = { Result.failure(IllegalStateException("Account with this email already exists")) } }
        val second = AuthViewModel(failing)
        try {
            second.onGoogleLogin("token")
            withTimeout(5_000) { second.loginError.first { it != null } }
            idle()
            assertEquals("Аккаунт с этим email уже существует. Используйте способ входа, с которым вы регистрировались.", second.loginError.value)
            second.onGoogleLoginError("anything")
            assertEquals("Не удалось войти через Google. Попробуйте ещё раз.", second.loginError.value)
            second.clearFieldErrors()
            assertNull(second.loginError.value)
        } finally { second.close() }
    }
}
