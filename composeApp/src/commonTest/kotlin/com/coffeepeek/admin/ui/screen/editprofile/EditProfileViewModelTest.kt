package com.coffeepeek.admin.ui.screen.editprofile

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Parity tests for the legacy edit-profile ViewModel (profile migration step 1). They pin production behaviour
 * before anything moves; the ViewModel runs on real dispatchers, so every assertion waits for its condition.
 */
class EditProfileViewModelTest {
    private val store = ViewModelStore()
    private val profile = UserProfile("Anna", "anna@example.com", "About", "https://img/a.jpg", 0, 3, 1)

    @AfterTest fun tearDown() = store.clear()

    private fun viewModel(repository: FakeUsers): EditProfileViewModel =
        EditProfileViewModel(repository).also { store.put("edit", it) }

    @Test fun cachedProfileFillsTheFormWithoutARequest() = runTest {
        val users = FakeUsers(cached = profile)
        val vm = viewModel(users)
        awaitState(vm) { !it.isLoading }
        assertEquals("Anna", vm.state.value.username)
        assertEquals("About", vm.state.value.about)
        assertEquals(0, users.refreshCalls)
        assertFalse(vm.state.value.hasChanges)
    }

    @Test fun missingCacheRefreshesAndAFailureShowsItsMessageOrTheFallback() = runTest {
        val users = FakeUsers(cached = null, refresh = Result.failure(IllegalStateException("boom")))
        val vm = viewModel(users)
        awaitState(vm) { it.error != null }
        assertEquals("boom", vm.state.value.error)
        assertFalse(vm.state.value.isLoading)

        val silent = viewModel(FakeUsers(cached = null, refresh = Result.failure(RuntimeException())))
        awaitState(silent) { it.error != null }
        assertEquals("Ошибка загрузки профиля", silent.state.value.error)
    }

    @Test fun usernameRulesAreMeasuredOnTheUntrimmedValue() = runTest {
        val vm = viewModel(FakeUsers(cached = profile))
        awaitState(vm) { !it.isLoading }
        vm.onUsernameChange("")
        assertEquals("Имя не может быть пустым", vm.state.value.usernameError)
        vm.onUsernameChange("A")
        assertEquals("Минимум 2 символа", vm.state.value.usernameError)
        vm.onUsernameChange(" A ")
        assertNull(vm.state.value.usernameError)
        vm.onUsernameChange("x".repeat(80))
        assertEquals(64, vm.state.value.username.length)
        assertNull(vm.state.value.usernameError)
        vm.onAboutChange("y".repeat(700))
        assertEquals(600, vm.state.value.about.length)
        assertNull(vm.state.value.aboutError)
    }

    @Test fun whitespaceOnlyEditsAreNotChanges() = runTest {
        val vm = viewModel(FakeUsers(cached = profile))
        awaitState(vm) { !it.isLoading }
        vm.onUsernameChange("  Anna  ")
        vm.onAboutChange(" About ")
        assertFalse(vm.state.value.hasChanges)
        assertFalse(vm.state.value.canSave)
        vm.onAboutChange("Other")
        assertTrue(vm.state.value.canSave)
    }

    @Test fun avatarOverFiveMegabytesIsRejectedWithoutBeingKept() = runTest {
        val vm = viewModel(FakeUsers(cached = profile))
        awaitState(vm) { !it.isLoading }
        vm.onAvatarPicked(PickedImage(ByteArray(5 * 1024 * 1024 + 1), "big.jpg"))
        assertEquals("Размер файла не должен превышать 5 МБ", vm.state.value.error)
        assertNull(vm.state.value.pendingAvatar)
        vm.onAvatarPicked(PickedImage(ByteArray(5 * 1024 * 1024), "ok.jpg"))
        assertNotNull(vm.state.value.pendingAvatar)
        assertNull(vm.state.value.error)
    }

    @Test fun saveRunsAvatarThenUsernameThenAboutTrimmedAndPopsBackOnlyOnSuccess() = runTest {
        val users = FakeUsers(cached = profile)
        val vm = viewModel(users)
        awaitState(vm) { !it.isLoading }
        vm.onAvatarPicked(PickedImage(byteArrayOf(1), "a.png", "image/png"))
        vm.onUsernameChange("  Boris ")
        vm.onAboutChange(" New ")
        val events = collectNavigation()
        vm.save()
        awaitState(vm) { !it.isSaving && users.calls.size == 3 }
        assertEquals(listOf("avatar:a.png:image/png", "username:Boris", "about:New"), users.calls)
        awaitEvents(events) { it.contains(Navigator.NavEvent.PopBack) }
        events.stop()
    }

    @Test fun saveStopsAtTheFirstFailureAndStaysOnTheScreen() = runTest {
        val users = FakeUsers(cached = profile, avatar = Result.failure(IllegalStateException("avatar down")))
        val vm = viewModel(users)
        awaitState(vm) { !it.isLoading }
        vm.onAvatarPicked(PickedImage(byteArrayOf(1), "a.png"))
        vm.onUsernameChange("Boris")
        val events = collectNavigation()
        vm.save()
        awaitState(vm) { !it.isSaving && it.error != null }
        assertEquals("avatar down", vm.state.value.error)
        assertEquals(listOf("avatar:a.png:image/jpeg"), users.calls)
        delay(200)
        assertFalse(events.seen.contains(Navigator.NavEvent.PopBack))
        events.stop()
    }

    /** Recorded as-is (owner decision 2026-10-20: this is fixed during the migration, then this test flips). */
    @Test fun afterAnAvatarSuccessAndANameFailureARetryUploadsTheAvatarAgain() = runTest {
        val users = FakeUsers(cached = profile, username = Result.failure(IllegalStateException("name taken")))
        val vm = viewModel(users)
        awaitState(vm) { !it.isLoading }
        vm.onAvatarPicked(PickedImage(byteArrayOf(1), "a.png"))
        vm.onUsernameChange("Boris")
        vm.save()
        awaitState(vm) { !it.isSaving && it.error != null }
        assertNotNull(vm.state.value.pendingAvatar)
        vm.save()
        awaitState(vm) { !it.isSaving && users.calls.count { c -> c.startsWith("avatar") } == 2 }
        assertEquals(2, users.calls.count { it.startsWith("avatar") })
    }

    private suspend fun awaitState(vm: EditProfileViewModel, condition: (EditProfileUiState) -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition(vm.state.value)) delay(10) } }

    private class Navigation(val scope: CoroutineScope, private val events: MutableStateFlow<List<Navigator.NavEvent>>) {
        val seen: List<Navigator.NavEvent> get() = events.value
        fun stop() = scope.cancel()
    }

    private suspend fun collectNavigation(): Navigation {
        val scope = CoroutineScope(Dispatchers.Default)
        val seen = MutableStateFlow<List<Navigator.NavEvent>>(emptyList())
        scope.launch { Navigator.navigationEvents.collect { event -> seen.update { it + event } } }
        withContext(Dispatchers.Default) { delay(100) }
        return Navigation(scope, seen)
    }

    private suspend fun awaitEvents(n: Navigation, condition: (List<Navigator.NavEvent>) -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition(n.seen)) delay(10) } }

    private class FakeUsers(
        cached: UserProfile?,
        private val refresh: Result<UserProfile> = Result.failure(IllegalStateException("not cached")),
        private val avatar: Result<Unit> = Result.success(Unit),
        private val username: Result<Unit> = Result.success(Unit),
        private val about: Result<Unit> = Result.success(Unit),
    ) : UserRepository {
        private val profile = MutableStateFlow(cached)
        private val recorded = MutableStateFlow<List<String>>(emptyList())
        private val refreshes = MutableStateFlow(0)
        val calls: List<String> get() = recorded.value
        val refreshCalls: Int get() = refreshes.value
        private fun record(call: String) = recorded.update { it + call }

        override fun observeProfile(): StateFlow<UserProfile?> = profile
        override suspend fun refreshProfile(): Result<UserProfile> { refreshes.update { it + 1 }; return refresh }
        override suspend fun getMe(): Result<UserProfile> = refresh
        override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = Result.success(null)
        override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("not used")
        override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("not used")
        override suspend fun updateUsername(username: String): Result<Unit> { record("username:$username"); return this.username }
        override suspend fun updateAbout(about: String): Result<Unit> { record("about:$about"); return this.about }
        override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> {
            record("avatar:${photo.fileName}:${photo.contentType}"); return avatar
        }
    }
}
