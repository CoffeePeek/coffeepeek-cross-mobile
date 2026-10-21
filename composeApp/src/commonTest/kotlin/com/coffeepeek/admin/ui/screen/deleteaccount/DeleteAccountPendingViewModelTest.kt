package com.coffeepeek.admin.ui.screen.deleteaccount

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Parity tests for the legacy pending-deletion ViewModel (profile migration step 1).
 * Owner decision 2026-10-20: the migrated screen must not request deletion by merely opening; the test
 * `openingWithoutARequestIssuesTheDeletionRequestItself` records today's behaviour and flips with that change.
 * The session-end redirect (`Navigator.openLoginAfterSessionEnd`) writes a private flag, so it is not asserted here.
 */
class DeleteAccountPendingViewModelTest {
    private val store = ViewModelStore()
    private val profile = UserProfile("Anna", "anna@example.com", null, null, 0, 0, 0)
    private val past = "2020-01-01T00:00:00Z"
    private val future = "2999-01-01T00:00:00Z"

    @AfterTest fun tearDown() = store.clear()

    private fun viewModel(users: FakeUsers): DeleteAccountPendingViewModel =
        DeleteAccountPendingViewModel(users, FakeSessions()).also { store.put("delete", it) }

    @Test fun anExistingRequestIsShownWithoutIssuingANewOne() = runTest {
        val users = FakeUsers(existing = Result.success(request(future)))
        val vm = viewModel(users)
        await(vm) { !it.isLoading }
        assertEquals(0, users.requests)
        assertEquals("anna@example.com", vm.uiState.value.email)
        assertFalse(vm.uiState.value.resendAvailable)
    }

    @Test fun openingWithoutARequestIssuesTheDeletionRequestItself() = runTest {
        val users = FakeUsers(existing = Result.success(null), created = Result.success(request(future)))
        val vm = viewModel(users)
        await(vm) { !it.isLoading }
        assertEquals(1, users.requests)
        assertFalse(vm.uiState.value.resendAvailable)
        delay(1_300)
        assertEquals(1, users.requests)
    }

    @Test fun statusLoadFailureShowsItsMessageOrTheFallbackAndSendsNothing() = runTest {
        val users = FakeUsers(existing = Result.failure(RuntimeException()))
        val vm = viewModel(users)
        await(vm) { !it.isLoading }
        assertEquals("Ошибка загрузки статуса", vm.uiState.value.error)
        assertEquals(0, users.requests)
    }

    @Test fun aFailedInitialRequestShowsTheSendFallback() = runTest {
        val users = FakeUsers(existing = Result.success(null), created = Result.failure(RuntimeException()))
        val vm = viewModel(users)
        await(vm) { !it.isLoading }
        assertEquals("Не удалось отправить письмо", vm.uiState.value.error)
    }

    @Test fun resendIsGatedUntilTheServerTimeAndBlankOrUnparsableMeansAvailable() = runTest {
        val waiting = viewModel(FakeUsers(existing = Result.success(request(future))))
        await(waiting) { !it.isLoading }
        assertFalse(waiting.uiState.value.resendAvailable)

        for (at in listOf(past, "", "not a date", "2020-01-01T00:00:00")) {
            val vm = viewModel(FakeUsers(existing = Result.success(request(at))))
            await(vm) { !it.isLoading }
            assertTrue(vm.uiState.value.resendAvailable, "'$at' must allow resend")
        }
    }

    @Test fun resendSendsWhenAvailableAndIsGatedAfterwards() = runTest {
        val users = FakeUsers(existing = Result.success(request(past)), created = Result.success(request(future)))
        val vm = viewModel(users)
        await(vm) { !it.isLoading && it.resendAvailable }
        vm.resendEmail()
        await(vm) { !it.isResending && users.requests >= 1 }
        delay(200)
        assertEquals(1, users.requests)
        assertFalse(vm.uiState.value.resendAvailable)
        vm.resendEmail()
        delay(200)
        assertEquals(1, users.requests)
    }

    private fun request(resendAt: String) = AccountDeletionRequest("id", "pending", future, resendAt)

    private suspend fun await(vm: DeleteAccountPendingViewModel, condition: (DeleteAccountPendingUiState) -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition(vm.uiState.value)) delay(10) } }

    private class FakeSessions : SessionRepository {
        private val session = MutableStateFlow<Session?>(Session("token", null, "user"))
        override fun peekSession() = session.value
        override fun applySession(session: Session?) { this.session.value = session }
        override fun isActiveSession(session: Session?) = session != null
        override suspend fun getSession() = session.value
        override suspend fun persistSession(session: Session?) { this.session.value = session }
        override suspend fun saveSession(session: Session?) { this.session.value = session }
        override suspend fun warmCache() = Unit
        override fun observeSession(): Flow<Session?> = session
        override suspend fun isLoggedIn() = session.value != null
    }

    private inner class FakeUsers(
        private val existing: Result<AccountDeletionRequest?>,
        private val created: Result<AccountDeletionRequest> = Result.failure(IllegalStateException("none")),
    ) : UserRepository {
        private val count = MutableStateFlow(0)
        val requests: Int get() = count.value

        override fun observeProfile(): StateFlow<UserProfile?> = MutableStateFlow(profile)
        override suspend fun refreshProfile() = Result.success(profile)
        override suspend fun getMe() = Result.success(profile)
        override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = Result.success(null)
        override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> { count.update { it + 1 }; return created }
        override suspend fun getAccountDeletionRequest() = existing
        override suspend fun updateUsername(username: String) = Result.success(Unit)
        override suspend fun updateAbout(about: String) = Result.success(Unit)
        override suspend fun updateAvatar(photo: PendingPhotoUpload) = Result.success(Unit)
    }
}
