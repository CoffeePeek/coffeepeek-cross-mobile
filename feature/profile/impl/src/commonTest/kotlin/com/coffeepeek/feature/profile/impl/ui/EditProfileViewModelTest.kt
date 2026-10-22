package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileError
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

    @Test fun missingCacheRefreshesAndFailuresDoNotExposeServerMessages() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts(own = null).apply { load = { Result.failure(IllegalStateException("boom")) } }
        val vm = model(accounts); runCurrent()
        assertEquals(EditProfileError.Load, vm.state.value.error); assertFalse(vm.state.value.isLoading)
        assertEquals(1, accounts.loads)

        val silent = model(FakeProfileAccounts(own = null).apply { load = { Result.failure(RuntimeException()) } }); runCurrent()
        assertEquals(EditProfileError.Load, silent.state.value.error)
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
        assertEquals(EditProfileError.AvatarTooLarge, vm.state.value.error); assertNull(vm.state.value.pendingAvatar)
        vm.act(EditProfileAction.AvatarPicked(image(5 * 1024 * 1024)))
        assertNotNull(vm.state.value.pendingAvatar); assertNull(vm.state.value.error)
    }

    @Test fun saveRunsAvatarThenUsernameThenAboutTrimmedAndReportsSavedOnlyOnSuccess() = runTest(dispatcher) {
        val edit = FakeEdit(); val vm = model(edit = edit); runCurrent()
        val events = mutableListOf<EditProfileEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.UsernameChanged("  Boris ")); vm.act(EditProfileAction.AboutChanged(" New "))
        vm.saveConfirmed()
        assertEquals(listOf("avatar:a.png:image/png", "username:Boris", "about:New"), edit.calls)
        assertFalse(vm.state.value.isSaving); assertEquals(listOf<EditProfileEvent>(EditProfileEvent.Saved), events)
        assertFalse(vm.state.value.hasChanges)
    }

    @Test fun saveStopsAtTheFirstFailureAndStaysOnTheScreen() = runTest(dispatcher) {
        val edit = FakeEdit(avatar = Result.failure(IllegalStateException("avatar down"))); val vm = model(edit = edit); runCurrent()
        val events = mutableListOf<EditProfileEvent>(); backgroundScope.launch(dispatcher) { vm.events.collect(events::add) }
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.UsernameChanged("Boris")); vm.saveConfirmed()
        assertEquals(EditProfileError.Avatar, vm.state.value.error); assertFalse(vm.state.value.isSaving)
        assertEquals(listOf("avatar:a.png:image/png"), edit.calls); assertTrue(events.isEmpty())
        assertNotNull(vm.state.value.pendingAvatar)
    }

    @Test fun failureFallbacksNameTheFailedStep() = runTest(dispatcher) {
        val avatar = model(edit = FakeEdit(avatar = Result.failure(RuntimeException(null as String?, RuntimeException("cause"))))); runCurrent()
        avatar.act(EditProfileAction.AvatarPicked(image())); avatar.act(EditProfileAction.Save)
        assertEquals(EditProfileError.Avatar, avatar.state.value.error)

        val bare = model(edit = FakeEdit(avatar = Result.failure(RuntimeException()))); runCurrent()
        bare.act(EditProfileAction.AvatarPicked(image())); bare.act(EditProfileAction.Save)
        assertEquals(EditProfileError.Avatar, bare.state.value.error)

        val name = model(edit = FakeEdit(username = Result.failure(RuntimeException()))); runCurrent()
        name.act(EditProfileAction.UsernameChanged("Boris")); name.saveConfirmed()
        assertEquals(EditProfileError.Username, name.state.value.error)

        val about = model(edit = FakeEdit(about = Result.failure(RuntimeException()))); runCurrent()
        about.act(EditProfileAction.AboutChanged("Other")); about.act(EditProfileAction.Save)
        assertEquals(EditProfileError.About, about.state.value.error)
    }

    /** Owner decision 2026-10-20: legacy re-uploaded the avatar (and the name) on retry; now finished steps are not repeated. */
    @Test fun aRetryAfterALaterFailureDoesNotRepeatTheStepsThatSucceeded() = runTest(dispatcher) {
        val edit = FakeEdit(about = Result.failure(IllegalStateException("too long"))); val vm = model(edit = edit); runCurrent()
        vm.act(EditProfileAction.AvatarPicked(image())); vm.act(EditProfileAction.UsernameChanged("Boris")); vm.act(EditProfileAction.AboutChanged("New"))
        vm.saveConfirmed()
        assertEquals(EditProfileError.About, vm.state.value.error)
        assertNull(vm.state.value.pendingAvatar); assertEquals("Boris", vm.state.value.originalUsername)
        edit.about = Result.success(Unit)
        vm.act(EditProfileAction.DismissError); vm.saveConfirmed()
        assertEquals(listOf("avatar:a.png:image/png", "username:Boris", "about:New", "about:New"), edit.calls)
    }

    @Test fun aSecondSaveWhileSavingIsIgnored() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); val edit = FakeEdit(gate = gate); val vm = model(edit = edit); runCurrent()
        vm.act(EditProfileAction.UsernameChanged("Boris")); vm.saveConfirmed(); vm.saveConfirmed()
        assertTrue(vm.state.value.isSaving)
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("username:Boris"), edit.calls)
    }

    @Test fun theUploadedAvatarUrlComesFromTheSharedCache() = runTest(dispatcher) {
        val accounts = FakeProfileAccounts()
        val edit = FakeEdit(); val vm = model(accounts, edit); runCurrent()
        edit.onAvatar = { accounts.publish(anna.copy(avatarUrl = "https://img/new.jpg")) }
        vm.act(EditProfileAction.AvatarPicked(image())); vm.saveConfirmed()
        assertEquals("https://img/new.jpg", vm.state.value.avatarUrl)
    }

    private fun EditProfileViewModel.saveConfirmed() {
        act(EditProfileAction.Save)
        act(EditProfileAction.ConfirmUsernameChange)
    }

    @Test fun changingTheUsernameWaitsForConfirmationBeforeAnyWrites() = runTest(dispatcher) {
        val edit = FakeEdit()
        val vm = model(edit = edit)
        runCurrent()
        vm.act(EditProfileAction.UsernameChanged("  Boris "))
        vm.act(EditProfileAction.AvatarPicked(image()))
        vm.act(EditProfileAction.AboutChanged("New"))
        vm.act(EditProfileAction.Save)
        assertEquals("Boris", vm.state.value.usernameToConfirm)
        assertTrue(edit.calls.isEmpty())
        assertFalse(vm.state.value.isSaving)
        vm.act(EditProfileAction.Save)
        assertTrue(edit.calls.isEmpty())
        vm.act(EditProfileAction.ConfirmUsernameChange)
        assertEquals(listOf("avatar:a.png:image/png", "username:Boris", "about:New"), edit.calls)
        assertNull(vm.state.value.usernameToConfirm)
    }

    @Test fun cancellingConfirmationKeepsTheDraftAndMakesNoRequests() = runTest(dispatcher) {
        val edit = FakeEdit()
        val vm = model(edit = edit)
        runCurrent()
        vm.act(EditProfileAction.UsernameChanged("Boris"))
        vm.act(EditProfileAction.AboutChanged("New"))
        vm.act(EditProfileAction.AvatarPicked(image()))
        vm.act(EditProfileAction.Save)
        vm.act(EditProfileAction.CancelUsernameChange)
        vm.act(EditProfileAction.ConfirmUsernameChange)
        assertTrue(edit.calls.isEmpty())
        assertNull(vm.state.value.usernameToConfirm)
        assertEquals("Boris", vm.state.value.username)
        assertEquals("New", vm.state.value.about)
        assertNotNull(vm.state.value.pendingAvatar)
    }

    @Test fun avatarAndAboutSaveWithoutConfirmationWhenTheTrimmedUsernameIsUnchanged() = runTest(dispatcher) {
        val edit = FakeEdit()
        val vm = model(edit = edit)
        runCurrent()
        vm.act(EditProfileAction.UsernameChanged(" Anna "))
        vm.act(EditProfileAction.AvatarPicked(image()))
        vm.act(EditProfileAction.AboutChanged("New"))
        vm.act(EditProfileAction.Save)
        assertNull(vm.state.value.usernameToConfirm)
        assertEquals(listOf("avatar:a.png:image/png", "about:New"), edit.calls)
    }

    @Test fun changingTheDraftInvalidatesThePreviousUsernameConfirmation() = runTest(dispatcher) {
        val edit = FakeEdit()
        val vm = model(edit = edit)
        runCurrent()
        vm.act(EditProfileAction.UsernameChanged("Boris"))
        vm.act(EditProfileAction.Save)
        vm.act(EditProfileAction.UsernameChanged("Charlie"))
        vm.act(EditProfileAction.ConfirmUsernameChange)
        assertTrue(edit.calls.isEmpty())
        assertNull(vm.state.value.usernameToConfirm)
        vm.act(EditProfileAction.Save)
        assertEquals("Charlie", vm.state.value.usernameToConfirm)
    }

    @Test fun limitFailureKeepsTheDraftAndRetryDateAndDoesNotWriteAbout() = runTest(dispatcher) {
        val retryAt = "2026-10-29T17:12:31.5305240Z"
        val edit = FakeEdit(username = Result.failure(UsernameChangeFailure(UsernameChangeFailure.Reason.LimitReached, retryAt)))
        val vm = model(edit = edit)
        runCurrent()
        vm.act(EditProfileAction.UsernameChanged("Boris"))
        vm.act(EditProfileAction.AboutChanged("New"))
        vm.saveConfirmed()
        assertEquals(EditProfileError.UsernameLimit, vm.state.value.error)
        assertEquals(retryAt, vm.state.value.usernameRetryAtUtc)
        assertEquals("Anna", vm.state.value.originalUsername)
        assertEquals(listOf("username:Boris"), edit.calls)
        assertFalse(vm.state.value.isSaving)
    }

    @Test fun aFailedUsernameChangeNeedsConfirmationAgainAfterDismissingTheError() = runTest(dispatcher) {
        val edit = FakeEdit(username = Result.failure(UsernameChangeFailure(UsernameChangeFailure.Reason.LimitReached)))
        val vm = model(edit = edit)
        runCurrent()
        vm.act(EditProfileAction.UsernameChanged("Boris"))
        vm.saveConfirmed()
        vm.act(EditProfileAction.DismissError)
        assertNull(vm.state.value.error)
        assertNull(vm.state.value.usernameRetryAtUtc)
        vm.act(EditProfileAction.Save)
        assertEquals("Boris", vm.state.value.usernameToConfirm)
        assertEquals(1, edit.calls.size)
    }

    @Test fun usernameFailuresUseTypedPresentationErrors() = runTest(dispatcher) {
        val cases = listOf(
            UsernameChangeFailure.Reason.Taken to EditProfileError.UsernameTaken,
            UsernameChangeFailure.Reason.Invalid to EditProfileError.InvalidUsername,
            UsernameChangeFailure.Reason.SignInRequired to EditProfileError.SignInRequired,
            UsernameChangeFailure.Reason.Unavailable to EditProfileError.Username,
        )
        for ((reason, expected) in cases) {
            val vm = model(edit = FakeEdit(username = Result.failure(UsernameChangeFailure(reason))))
            runCurrent()
            vm.act(EditProfileAction.UsernameChanged("Boris"))
            vm.saveConfirmed()
            assertEquals(expected, vm.state.value.error)
        }
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
