package com.coffeepeek.feature.profile.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.impl.ui.EditProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.FakeProfileAccounts
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileError
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeEditProfileSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val accounts = FakeProfileAccounts()
    private val writes = mutableListOf<String>()
    private val edit = object : ProfileEditPort {
        override suspend fun updateAvatar(image: ProfileAvatarImage) = Result.success(Unit).also { writes += "avatar" }
        override suspend fun updateUsername(username: String) = Result.success(Unit).also { writes += "username" }
        override suspend fun updateAbout(about: String) = Result.success(Unit).also { writes += "about" }
    }
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { store.clear(); Dispatchers.resetMain() }
    private fun session(account: ProfileAccountPort = accounts): NativeEditProfileSession {
        val model = EditProfileViewModel(account, edit)
        store.put("editor", model)
        return NativeEditProfileSession(model, account, store::clear)
    }

    private fun failingAccount(failure: CompletableDeferred<Unit>) = object : ProfileAccountPort by accounts {
        override fun observeActiveAccount(): Flow<String?> = flow {
            emit(accounts.activeAccount())
            failure.await()
            error("Account observation failed")
        }
    }

    @Test fun revokedSnapshotContainsNoPrivateDraftOrPendingDialog() {
        val state = EditProfileState(username = "Private", about = "Notes", avatarUrl = "private://image",
            pendingAvatar = ProfileAvatarImage("avatar", "image/png", byteArrayOf(1)),
            usernameToConfirm = "Secret", error = EditProfileError.UsernameLimit, usernameRetryAtUtc = "date")
        val snapshot = state.nativeSnapshot(revoked = true)
        assertTrue(snapshot.revoked)
        assertEquals("", snapshot.username)
        assertEquals("", snapshot.about)
        assertNull(snapshot.avatarUrl)
        assertNull(snapshot.avatar)
        assertNull(snapshot.usernameToConfirm)
        assertNull(snapshot.errorMessageKey)
        assertNull(snapshot.retryAtUtc)
        assertFalse(snapshot.canSave)
    }

    @Test fun replacedAccountCannotEditOrSaveOldDraft() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        accounts.signIn("replacement")
        session.usernameChanged("New_name")
        session.aboutChanged("New notes")
        session.avatarPicked(ProfileAvatarImage("photo", "image/png", byteArrayOf(1)))
        session.save()
        session.confirmUsername()
        runCurrent()
        assertTrue(session.current().revoked)
        assertTrue(writes.isEmpty())
        accounts.signIn("a")
        assertFalse(session.isCurrent())
    }

    @Test fun queuedSaveCannotWriteAfterAccountReplacedBeforeVmHandlesAction() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        session.aboutChanged("Updated notes")
        runCurrent()
        assertTrue(session.current().canSave)
        session.save()
        accounts.signIn("replacement")
        runCurrent()
        assertTrue(writes.isEmpty())
    }

    @Test fun nativeSaveUsesSameUsernameConfirmationAndSingleWriter() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        session.usernameChanged("New_name")
        runCurrent()
        session.save()
        runCurrent()
        assertEquals("New_name", session.current().usernameToConfirm)
        assertTrue(writes.isEmpty())
        session.confirmUsername()
        runCurrent()
        assertEquals(listOf("username"), writes)
        assertFalse(session.current().canSave)
    }

    @Test fun avatarIsOwnedBeforeThePlatformPickerClearsItsTemporaryBuffer() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        val bytes = byteArrayOf(1, 2, 3)
        session.avatarPicked(ProfileAvatarImage("photo", "image/png", bytes))
        bytes.fill(0)
        runCurrent()
        assertContentEquals(byteArrayOf(1, 2, 3), session.current().avatar?.bytes)
    }

    @Test fun closeClearsTheStoreAndRejectsQueuedDraftWritesAndRepeatedDisposal() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        session.aboutChanged("Pending draft")
        session.close()
        session.close()
        session.save()
        session.usernameChanged("Late username")
        runCurrent()
        assertFalse(session.isCurrent())
        assertTrue(session.current().revoked)
        assertTrue(writes.isEmpty())
        assertEquals("", session.current().about)
    }

    @Test fun accountFlowFailurePublishesSanitizedStateToTheExistingUiObserver() = runTest(dispatcher) {
        val failure = CompletableDeferred<Unit>()
        val session = session(failingAccount(failure))
        val snapshots = mutableListOf<NativeEditProfileState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.state.toList(snapshots) }
        runCurrent()
        session.aboutChanged("Private draft")
        runCurrent()
        assertEquals("Private draft", snapshots.last().about)
        failure.complete(Unit)
        runCurrent()
        assertTrue(snapshots.last().revoked)
        assertEquals("", snapshots.last().username)
        assertEquals("", snapshots.last().about)
        assertNull(snapshots.last().avatar)
        assertFalse(session.isCurrent())
    }

    @Test fun cancellingOneUiObserverDoesNotDisposeTheDraftOwner() = runTest(dispatcher) {
        val session = session()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.state.toList(mutableListOf()) }
        runCurrent()
        session.aboutChanged("Keep draft")
        runCurrent()
        observer.cancelAndJoin()
        assertTrue(session.isCurrent())
        assertEquals("Keep draft", session.current().about)
    }
}
