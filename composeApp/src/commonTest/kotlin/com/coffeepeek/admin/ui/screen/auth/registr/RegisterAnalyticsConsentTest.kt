package com.coffeepeek.admin.ui.screen.auth.registr

import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.admin.ui.Navigator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisterAnalyticsConsentTest {
    @Test fun registrationSucceedsWithAnalyticsUnchecked() = runBlocking {
        val calls = Channel<String>(Channel.UNLIMITED)
        val vm = RegisterViewModel(ConsentAuthRepository(calls)) { calls.send("consent:$it") }
        try {
            assertFalse(vm.isAnalyticsAccepted.value)
            vm.onNameChange("Coffee User")
            vm.onEmailChange("person@example.invalid")
            vm.onPasswordChange("TestPassword123")
            vm.onTermsCheckedChange(true)
            vm.onRegisterClick()
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Success } }
            assertEquals("consent:false", calls.receive())
            assertEquals("register", calls.receive())
        } finally { vm.close() }
    }

    @Test fun registrationKeepsAnalyticsChoiceSeparateFromTermsAndAcrossSteps() = runBlocking {
        val calls = Channel<String>(Channel.UNLIMITED)
        val vm = RegisterViewModel(ConsentAuthRepository(calls)) { calls.send("consent:$it") }
        try {
            vm.onEmailChange("person@example.invalid")
            vm.onAnalyticsCheckedChange(true)
            assertFalse(vm.isTermsAccepted.value)
            vm.onContinueEmail()
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Details } }
            vm.onBack()
            assertEquals(true, vm.isAnalyticsAccepted.value)
            vm.onNameChange("Coffee User")
            vm.onPasswordChange("TestPassword123")
            vm.onTermsCheckedChange(true)
            vm.onRegisterClick()
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Success } }
            assertEquals("consent:true", calls.receive())
            assertEquals("register", calls.receive())
        } finally { vm.close() }
    }

    @Test fun socialLoginDoesNotApplyConsentFromTheHiddenSecondRegistrationStep() = runBlocking {
        for (accepted in listOf(false, true)) {
            val calls = Channel<String>(Channel.UNLIMITED)
            val vm = RegisterViewModel(ConsentAuthRepository(calls)) { calls.send("consent:$it") }
            try {
                vm.onAnalyticsCheckedChange(accepted)
                val navigation = async(start = CoroutineStart.UNDISPATCHED) {
                    Navigator.navigationEvents.first { it == Navigator.NavEvent.NavigateTo(Navigator.Screen.Main) }
                }
                vm.onGoogleLogin("test-token")
                withTimeout(5_000) {
                    navigation.await()
                    assertEquals("google", calls.receive())
                }
                assertTrue(calls.tryReceive().isFailure, "Social login must preserve the stored consent")
            } finally { vm.close() }
        }
    }
}

private class ConsentAuthRepository(private val calls: Channel<String>) : AuthRepository {
    override suspend fun register(userName: String, email: String, password: String): Result<Unit> {
        calls.send("register")
        return Result.success(Unit)
    }
    override suspend fun googleLogin(idToken: String): Result<Session> {
        calls.send("google")
        return Result.success(Session("test-session", userId = "backend-user"))
    }
    override suspend fun isEmailTaken(email: String) = Result.success(false)
    override suspend fun login(email: String, password: String): Result<Session> = error("Unused")
    override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> = error("Unused")
    override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> = error("Unused")
    override suspend fun logout(): Result<Unit> = error("Unused")
}
