package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.UsernameError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

/** Port of the legacy `EditProfileViewModelTest`; the partial-save test flipped with the owner-approved fix. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(accounts: FakeProfileAccounts = FakeProfileAccounts(), edit: FakeEdit = FakeEdit()) =
        EditProfileViewModel(accounts, edit).also { store.put("edit-${nextKey++}", it) }

    private fun EditProfileViewModel.act(action: EditProfileAction) { onAction(action); dispatcher.scheduler.runCurrent() }
    private fun image(size: Int = 1, name: String = "a.png", type: String = "image/png") = ProfileAvatarImage(name, type, ByteArray(size))

    @Test fun cachedProfileFillsTheFormWithoutARequest() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts()
        val vm = model(accounts); runCurrent()
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("Anna", state.username); assertEquals("About", state.about)
        assertEquals(0, accounts.loads)
        assertFalse(state.hasChanges)
    }

    @Test fun missingCacheRefreshesAndAFailureShowsItsMessageOrTheFallback() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts(own = null).apply { load = { Result.failure(IllegalStateException("boom")) } }
        val vm = model(accounts); runCurrent()
        assertEquals("boom", vm.state.value.error); assertFalse(vm.state.value.isLoading)
        assertEquals(1, accounts.loads)

        val silent = model(FakeProfileAccounts(own = null).apply { load = { Result.failure(RuntimeException()) } }); runCurrent()
        assertEquals("Ошибка загрузки профиля", silent.state.value.error)
    }

    @Test fun usernameRulesAreMeasuredOnTheUntrimmedValue() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.act(EditProfileAction.UsernameChanged("")); assertEquals(UsernameError.Empty, vm.state.value.usernameError)
        vm.act(EditProfileAction.UsernameChanged("A")); assertEquals(UsernameError.TooShort, vm.state.value.usernameError)
        vm.act(EditProfileAction.UsernameChanged(" A ")); assertNull(vm.state.value.usernameError)
        vm.act(EditProfileAction.UsernameChanged("x".repeat(80)))
        assertEquals(64, vm.state.value.username.length); assertNull(vm.state.value.usernameError)
        vm.act(EditProfileAction.AboutChanged("y".repeat(700)))
        assertEquals(600, vm.state.value.about.length); assertFalse(vm.state.value.aboutTooLong)
    }

    @Test fun whitespaceOnlyEditsAreNotChanges() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.act(EditProfileAction.UsernameChanged("  Anna  ")); vm.act(EditProfileAction.AboutChanged(" About "))
        assertFalse(vm.state.value.hasChanges); assertFalse(vm.state.value.canSave)
        vm.act(EditProfileAction.AboutChanged("Other")); assertTrue(vm.state.value.canSave)
    }

    @Test fun avatarOverFiveMegabytesIsRejectedWithoutBeingKept() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.act(EditProfileAction.AvatarPicked(image(5 * 1024 * 1024 + 1)))
        assertEquals("Размер файла не должен превышать 5 МБ", vm.state.value.error); assertNull(vm.state.value.pendingAvatar)
        vm.act(EditProfileAction.AvatarPicked(image(5 * 1024 * 1024)))
        assertNotNull(vm.state.value.pendingAvatar); assertNull(vm.state.value.error)
    }

    @Test fun saveRunsAvatarThenUsernameThenAboutTrimmedAndReportsSavedOnlyOnSuccess() = runTest(dispatcher) {
        val edit = FakeEdit(); val vm = model(edit = edit); runCurrent()
        val events = mutableListOf<EditProfileEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.UsernameChanged("  Boris ")); vm.act(EditProfileAction.AboutChanged(" New "))
        vm.act(EditProfileAction.Save)
        assertEquals(listOf("avatar:a.png:image/png", "username:Boris", "about:New"), edit.calls)
        assertFalse(vm.state.value.isSaving); assertEquals(listOf<EditProfileEvent>(EditProfileEvent.Saved), events)
        assertFalse(vm.state.value.hasChanges)
    }

    @Test fun saveStopsAtTheFirstFailureAndStaysOnTheScreen() = runTest(dispatcher) {
        val edit = FakeEdit(avatar = Result.failure(IllegalStateException("avatar down"))); val vm = model(edit = edit); runCurrent()
        val events = mutableListOf<EditProfileEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.UsernameChanged("Boris")); vm.act(EditProfileAction.Save)
        assertEquals("avatar down", vm.state.value.error); assertFalse(vm.state.value.isSaving)
        assertEquals(listOf("avatar:a.png:image/png"), edit.calls); assertTrue(events.isEmpty())
        assertNotNull(vm.state.value.pendingAvatar)
    }

    @Test fun failureFallbacksNameTheFailedStep() = runTest(dispatcher) {
        val avatar = model(edit = FakeEdit(avatar = Result.failure(RuntimeException(null as String?, RuntimeException("cause"))))); runCurrent()
        avatar.act(EditProfileAction.AvatarPicked(image())); avatar.act(EditProfileAction.Save)
        assertEquals("cause", avatar.state.value.error)

        val bare = model(edit = FakeEdit(avatar = Result.failure(RuntimeException()))); runCurrent()
        bare.act(EditProfileAction.AvatarPicked(image())); bare.act(EditProfileAction.Save)
        assertEquals("Ошибка обновления аватара", bare.state.value.error)

        val name = model(edit = FakeEdit(username = Result.failure(RuntimeException()))); runCurrent()
        name.act(EditProfileAction.UsernameChanged("Boris")); name.act(EditProfileAction.Save)
        assertEquals("Ошибка обновления имени", name.state.value.error)

        val about = model(edit = FakeEdit(about = Result.failure(RuntimeException()))); runCurrent()
        about.act(EditProfileAction.AboutChanged("Other")); about.act(EditProfileAction.Save)
        assertEquals("Ошибка обновления описания", about.state.value.error)
    }

    /** Owner decision 2026-10-20: legacy re-uploaded the avatar (and the name) on retry; now finished steps are not repeated. */
    @Test fun aRetryAfterALaterFailureDoesNotRepeatTheStepsThatSucceeded() = runTest(dispatcher) {
        val edit = FakeEdit(about = Result.failure(IllegalStateException("too long"))); val vm = model(edit = edit); runCurrent()
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.UsernameChanged("Boris")); vm.act(EditProfileAction.AboutChanged("New"))
        vm.act(EditProfileAction.Save)
        assertEquals("too long", vm.state.value.error)
        assertNull(vm.state.value.pendingAvatar); assertEquals("Boris", vm.state.value.originalUsername)
        edit.about = Result.success(Unit)
        vm.act(EditProfileAction.DismissError); vm.act(EditProfileAction.Save)
        assertEquals(listOf("avatar:a.png:image/png", "username:Boris", "about:New", "about:New"), edit.calls)
    }

    @Test fun aSecondSaveWhileSavingIsIgnored() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); val edit = FakeEdit(gate = gate); val vm = model(edit = edit); runCurrent()
        vm.act(EditProfileAction.UsernameChanged("Boris")); vm.act(EditProfileAction.Save); vm.act(EditProfileAction.Save)
        assertTrue(vm.state.value.isSaving)
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("username:Boris"), edit.calls)
    }

    @Test fun theUploadedAvatarUrlComesFromTheSharedCache() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts()
        val edit = FakeEdit(); val vm = model(accounts, edit); runCurrent()
        edit.onAvatar = { accounts.publish(anna.copy(avatarUrl = "https://img/new.jpg")) }
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.Save)
        assertEquals("https://img/new.jpg", vm.state.value.avatarUrl)
    }

    private class FakeEdit(
        var avatar: Result<Unit> = Result.success(Unit), var username: Result<Unit> = Result.success(Unit),
        var about: Result<Unit> = Result.success(Unit), private val gate: CompletableDeferred<Unit>? = null,
    ) : ProfileEditPort {
        val calls = mutableListOf<String>()
        var onAvatar: () -> Unit = {}
        override suspend fun updateAvatar(image: ProfileAvatarImage): Result<Unit> { calls += "avatar:${image.fileName}:${image.contentType}"; gate?.await(); onAvatar(); return avatar }
        override suspend fun updateUsername(username: String): Result<Unit> { calls += "username:$username"; gate?.await(); return this.username }
        override suspend fun updateAbout(about: String): Result<Unit> { calls += "about:$about"; gate?.await(); return this.about }
    }
}
