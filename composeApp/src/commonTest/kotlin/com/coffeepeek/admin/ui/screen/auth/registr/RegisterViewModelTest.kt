package com.coffeepeek.admin.ui.screen.auth.registr

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.LoadingHandler
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The legacy registration view model as it behaves today: steps, email check and field errors. The texts that come from compose
 * resources (taken e-mail, the single submit message and its priority) cannot be read in a plain unit test, so they are
 * characterized from the code and on the device (see feature/auth/LEGACY_INVENTORY.md, C2 and C4).
 */
class RegisterViewModelTest {
    private class FakeAuth(var taken: Boolean = false) : AuthRepository {
        val registrations = mutableListOf<Triple<String, String, String>>()
        val emailChecks = mutableListOf<String>()
        override suspend fun isEmailTaken(email: String): Result<Boolean> { emailChecks += email; return Result.success(taken) }
        override suspend fun register(userName: String, email: String, password: String): Result<Unit> {
            registrations += Triple(userName, email, password); return Result.success(Unit)
        }
        override suspend fun login(email: String, password: String): Result<Session> = error("Unused")
        override suspend fun googleLogin(idToken: String): Result<Session> = error("Unused")
        override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> = error("Unused")
        override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> = error("Unused")
        override suspend fun logout(): Result<Unit> = error("Unused")
    }

    private val consents = mutableListOf<Boolean>()
    private fun viewModel(auth: FakeAuth) = RegisterViewModel(auth) { consents += it }

    private suspend fun idle() = withTimeout(5_000) { LoadingHandler.isLoading.first { !it } }

    @Test fun anInvalidEmailStaysOnTheEmailStepWithoutAskingTheServer() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = viewModel(auth)
        try {
            vm.onEmailChange("nope")
            vm.onContinueEmail()
            assertEquals("Введите корректный email", vm.emailError.value)
            assertEquals(RegisterStep.Email, vm.step.value)
            assertTrue(auth.emailChecks.isEmpty())
            vm.onEmailChange("nope2")
            assertNull(vm.emailError.value, "Editing clears the email error")
        } finally { vm.close() }
    }

    @Test fun aFreeEmailIsTrimmedCheckedOnceAndMovesToTheDetailsStep() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = viewModel(auth)
        try {
            vm.onEmailChange(" person@example.com ")
            vm.onContinueEmail()
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Details } }
            idle()
            assertEquals(listOf("person@example.com"), auth.emailChecks)
            assertNull(vm.emailError.value)
        } finally { vm.close() }
    }

    @Test fun anInvalidSubmitMarksTheFieldsAndSendsNothingNorSavesConsent() = runBlocking<Unit> {
        class Case(val email: String, val password: String, val emailError: String?, val passwordError: String?)
        val cases = listOf(
            Case("person@example.com", "short", null, "Пароль должен содержать как минимум 8 символов"),
            Case("bad", "TestPassword123", "Введите корректный email", null),
            Case("", "short", "Введите email", "Пароль должен содержать как минимум 8 символов"),
        )
        for (case in cases) {
            val auth = FakeAuth()
            val vm = viewModel(auth)
            try {
                vm.onNameChange("Coffee User")
                vm.onEmailChange(case.email)
                vm.onPasswordChange(case.password)
                vm.onTermsCheckedChange(true)
                vm.onAnalyticsCheckedChange(true)
                vm.onRegisterClick()
                withTimeout(5_000) {
                    vm.emailError.first { it == case.emailError }
                    vm.passwordError.first { it == case.passwordError }
                }
                idle()
                assertEquals(case.emailError, vm.emailError.value, case.email)
                assertEquals(case.passwordError, vm.passwordError.value, case.password)
                assertTrue(auth.registrations.isEmpty(), "Nothing is sent while a check fails")
                assertTrue(consents.isEmpty(), "Consent is saved only for a valid submission")
                assertEquals(RegisterStep.Email, vm.step.value)
            } finally { vm.close() }
        }
    }

    @Test fun aShortPasswordMarksItsFieldAndALongOneClearsIt() = runBlocking<Unit> {
        val vm = viewModel(FakeAuth())
        try {
            vm.onNameChange("Coffee User"); vm.onEmailChange("person@example.com"); vm.onPasswordChange("1234567")
            vm.onRegisterClick()
            withTimeout(5_000) { vm.passwordError.first { it != null } }
            idle()
            assertEquals("Пароль должен содержать как минимум 8 символов", vm.passwordError.value)
            vm.onPasswordChange("12345")
            assertEquals("Пароль должен содержать как минимум 8 символов", vm.passwordError.value, "While an error is shown it is validated live")
            vm.onPasswordChange("12345678")
            assertNull(vm.passwordError.value, "A valid value clears it")
            vm.onPasswordChange("1234567")
            assertNull(vm.passwordError.value, "Once cleared, nothing is shown until the next submit")
        } finally { vm.close() }
    }

    @Test fun backAndGoToLoginNavigateAsTheScreensExpect() = runBlocking<Unit> {
        val auth = FakeAuth()
        val vm = viewModel(auth)
        try {
            vm.onEmailChange("person@example.com")
            vm.onContinueEmail()
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Details } }
            idle()
            vm.onPasswordChange("x"); vm.onBack()
            assertEquals(RegisterStep.Email, vm.step.value)
            assertNull(vm.passwordError.value, "Going back clears the password error")

            val back = async(start = CoroutineStart.UNDISPATCHED) { Navigator.navigationEvents.first { it == Navigator.NavEvent.PopBack } }
            vm.onBack()
            withTimeout(5_000) { back.await() }

            val events = mutableListOf<Navigator.NavEvent>()
            val collector = async(start = CoroutineStart.UNDISPATCHED) {
                Navigator.navigationEvents.first { event -> events += event; event == Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth) }
            }
            vm.goToLogin()
            withTimeout(5_000) { collector.await() }
            assertEquals(listOf(Navigator.NavEvent.PopBack, Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth)), events)
        } finally { vm.close() }
    }
}
