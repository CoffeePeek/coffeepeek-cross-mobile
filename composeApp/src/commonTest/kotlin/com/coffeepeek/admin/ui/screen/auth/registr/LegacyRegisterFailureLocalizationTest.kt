package com.coffeepeek.admin.ui.screen.auth.registr

import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.utils.LoadingHandler
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacyRegisterFailureLocalizationTest {
    @BeforeTest fun clearErrors() = ErrorHandler.clearError()
    @AfterTest fun releaseErrors() = ErrorHandler.clearError()

    @Test fun aTakenEmailIsSemanticAndEditingClearsItWithoutResourceLoading() = runBlocking {
        val repository = FailureAuth().apply { emailResult = Result.success(true) }
        withModel(repository) { vm ->
            vm.onContinueEmail { belarusianRegistrationErrorCopy }
            withTimeout(5_000) { vm.emailTaken.first { it } }
            idle()
            assertEquals(RegisterStep.Email, vm.step.value)
            assertNull(vm.emailError.value)
            vm.onEmailChange("another@example.com")
            assertEquals(false, vm.emailTaken.value)
        }
    }

    @Test fun aBlankEmailCheckFailureUsesThePreparedBelarusianFallback() = runBlocking {
        val repository = FailureAuth().apply { emailResult = Result.failure(IllegalStateException()) }
        withModel(repository) { vm ->
            vm.onContinueEmail { belarusianRegistrationErrorCopy }
            withTimeout(5_000) { ErrorHandler.errorMessage.first { it == belarusianRegistrationErrorCopy.checkFailed } }
            idle()
            assertEquals(RegisterStep.Email, vm.step.value)
            assertNull(vm.emailError.value)
        }
    }

    @Test fun invalidRegistrationUsesTheCurrentCopyAndStaysOnEmailWithoutStartingARequest() = runBlocking {
        for (copy in listOf(russianRegistrationErrorCopy, belarusianRegistrationErrorCopy)) {
            val repository = FailureAuth()
            withModel(repository) { vm ->
                vm.onNameChange("")
                vm.onPasswordChange("x")
                vm.onTermsCheckedChange(false)
                vm.onRegisterClick { copy }
                assertEquals(copy.nameRequired, ErrorHandler.errorMessage.value)
                assertEquals(RegisterStep.Email, vm.step.value)
                assertEquals(0, repository.registrationCalls)
                assertFalse(LoadingHandler.isLoading.value)
            }
        }
    }

    @Test fun aFailedRegistrationReadsTheLatestPreparedCopyAndCanBeRetried() = runBlocking {
        val repository = FailureAuth().apply { registrationResult = Result.failure(IllegalStateException()); gate = CompletableDeferred() }
        var copy = russianRegistrationErrorCopy
        withModel(repository) { vm ->
            vm.onRegisterClick { copy }
            withTimeout(5_000) { repository.started.await() }
            copy = belarusianRegistrationErrorCopy
            repository.gate!!.complete(Unit)
            withTimeout(5_000) { ErrorHandler.errorMessage.first { it == copy.registrationFailed } }
            idle()
            assertEquals(RegisterStep.Email, vm.step.value)
            repository.registrationResult = Result.success(Unit)
            vm.onRegisterClick { copy }
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Success } }
            assertEquals(2, repository.registrationCalls)
        }
    }

    @Test fun backendProvidedRegistrationCopyIsPreservedVerbatim() = runBlocking {
        val backendMessage = "Account creation is temporarily unavailable"
        val repository = FailureAuth().apply { registrationResult = Result.failure(IllegalStateException(backendMessage)) }
        withModel(repository) { vm ->
            vm.onRegisterClick { belarusianRegistrationErrorCopy }
            withTimeout(5_000) { ErrorHandler.errorMessage.first { it == backendMessage } }
            idle()
            assertEquals(RegisterStep.Email, vm.step.value)
        }
    }

    @Test fun cancellationShowsNoFallbackAndReleasesTheSubmitGuard() = runBlocking {
        val repository = FailureAuth().apply { registrationResult = Result.failure(CancellationException("cancelled")); gate = CompletableDeferred() }
        withModel(repository) { vm ->
            vm.onRegisterClick { belarusianRegistrationErrorCopy }
            withTimeout(5_000) { repository.started.await() }
            assertTrue(LoadingHandler.isLoading.value)
            repository.gate!!.complete(Unit)
            idle()
            assertNull(ErrorHandler.errorMessage.value)
            assertEquals(RegisterStep.Email, vm.step.value)
            repository.registrationResult = Result.success(Unit)
            vm.onRegisterClick { russianRegistrationErrorCopy }
            withTimeout(5_000) { vm.step.first { it == RegisterStep.Success } }
            assertEquals(2, repository.registrationCalls)
        }
    }

    private suspend fun withModel(repository: AuthRepository, block: suspend (RegisterViewModel) -> Unit) {
        val vm = RegisterViewModel(repository) {}
        vm.onNameChange("Coffee User")
        vm.onEmailChange("person@example.com")
        vm.onPasswordChange("TestPassword123")
        vm.onTermsCheckedChange(true)
        try { block(vm) } finally { vm.close() }
    }

    private suspend fun idle() = withTimeout(5_000) { LoadingHandler.isLoading.first { !it } }
}

private class FailureAuth : AuthRepository {
    var emailResult = Result.success(false)
    var registrationResult: Result<Unit> = Result.success(Unit)
    var registrationCalls = 0
    var gate: CompletableDeferred<Unit>? = null
    val started = CompletableDeferred<Unit>()
    override suspend fun isEmailTaken(email: String) = emailResult
    override suspend fun register(userName: String, email: String, password: String): Result<Unit> {
        registrationCalls++
        started.complete(Unit)
        gate?.await()
        return registrationResult
    }
    override suspend fun login(email: String, password: String): Result<Session> = error("Unused")
    override suspend fun googleLogin(idToken: String): Result<Session> = error("Unused")
    override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> = error("Unused")
    override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> = error("Unused")
    override suspend fun logout(): Result<Unit> = error("Unused")
}
