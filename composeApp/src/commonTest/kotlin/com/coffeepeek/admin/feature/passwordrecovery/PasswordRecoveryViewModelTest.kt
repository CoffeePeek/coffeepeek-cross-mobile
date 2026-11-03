@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.feature.passwordrecovery

import com.coffeepeek.admin.feature.passwordrecovery.ui.message

import com.coffeepeek.admin.feature.passwordrecovery.ui.PasswordRecoveryError

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.feature.passwordrecovery.domain.PasswordRecoveryRepository
import com.coffeepeek.admin.feature.passwordrecovery.ui.PasswordRecoveryViewModel
import com.coffeepeek.admin.feature.passwordrecovery.ui.RecoveryStep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasswordRecoveryViewModelTest {
    private val validationLabels = com.coffeepeek.admin.ui.component.InputValidationLabels("Введите email", "Введите корректный email", "Введите пароль", "Пароль должен содержать как минимум %1\$s символов", "Некорректный формат телефона", "Укажите домен, например coffeepeek.by", "Некорректный Instagram")

    private val viewModels = ViewModelStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun successEndsAtEmailSentUntilAnotherEmailIsRequested() = runTest {
        val repository = RecordingRepository()
        val vm = PasswordRecoveryViewModel("user@example.com", repository)
        viewModels.put("recovery", vm)

        vm.submit()
        runCurrent()
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
        assertFalse(vm.state.value.busy)
        assertNull(vm.state.value.error)

        vm.submit()
        runCurrent()
        assertEquals(listOf("user@example.com"), repository.emails)

        vm.requestAnotherEmail()
        assertEquals(RecoveryStep.Email, vm.state.value.step)
        vm.onEmailChange("another@example.com")
        vm.submit()
        runCurrent()
        assertEquals(listOf("user@example.com", "another@example.com"), repository.emails)
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
    }

    @Test
    fun invalidEmailDoesNotRequestReset() = runTest {
        val repository = RecordingRepository()
        val vm = PasswordRecoveryViewModel("", repository)
        viewModels.put("recovery", vm)

        for (email in listOf("", "invalid-email")) {
            vm.onEmailChange(email)
            vm.submit()
            runCurrent()
            assertNotNull(vm.state.value.error)
            assertEquals(RecoveryStep.Email, vm.state.value.step)
            assertFalse(vm.state.value.busy)
        }
        assertTrue(repository.emails.isEmpty())
    }

    @Test
    fun failedRequestCanBeRetried() = runTest {
        var fail = true
        val repository = RecordingRepository {
            if (fail) Result.failure(IllegalStateException("Offline")) else Result.success(Unit)
        }
        val vm = PasswordRecoveryViewModel("user@example.com", repository)
        viewModels.put("recovery", vm)

        vm.submit()
        runCurrent()
        assertEquals(RecoveryStep.Email, vm.state.value.step)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.busy)

        fail = false
        vm.submit()
        assertNull(vm.state.value.error)
        runCurrent()
        assertEquals(2, repository.emails.size)
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun pendingRequestBlocksDoubleSubmitAndEdits() = runTest {
        val response = CompletableDeferred<Result<Unit>>()
        val repository = RecordingRepository { response.await() }
        val vm = PasswordRecoveryViewModel("user@example.com", repository)
        viewModels.put("recovery", vm)

        vm.submit()
        vm.submit()
        assertTrue(vm.state.value.busy)
        runCurrent()
        vm.submit()
        vm.onEmailChange("another@example.com")
        vm.requestAnotherEmail()
        runCurrent()
        assertEquals(listOf("user@example.com"), repository.emails)
        assertEquals("user@example.com", vm.state.value.email)
        assertTrue(vm.state.value.busy)

        response.complete(Result.success(Unit))
        runCurrent()
        assertEquals(RecoveryStep.EmailSent, vm.state.value.step)
        assertFalse(vm.state.value.busy)
    }

    private class RecordingRepository(
        private val response: suspend () -> Result<Unit> = { Result.success(Unit) },
    ) : PasswordRecoveryRepository {
        val emails = mutableListOf<String>()

        override suspend fun requestReset(email: String): Result<Unit> {
            emails += email
            return response()
        }
    }
    @Test fun recoveryFeedbackUsesPreparedBelarusianCopy() {
        val error = PasswordRecoveryError.Validation(com.coffeepeek.admin.utils.InputValidationIssue.EmailInvalid)
        assertEquals("Увядзіце карэктны email", error.message(validationLabels.copy(invalidEmail = "Увядзіце карэктны email"), "Памылка адпраўкі"))
        assertEquals("Памылка адпраўкі", PasswordRecoveryError.SendFailed.message(validationLabels, "Памылка адпраўкі"))
    }
}
