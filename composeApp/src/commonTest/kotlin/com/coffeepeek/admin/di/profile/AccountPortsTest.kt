@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.profile

import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountPortsTest {
    private class Users : UserRepository {
        val calls = mutableListOf<String>()
        var existing: Result<AccountDeletionRequest?> = Result.success(null)
        var created: Result<AccountDeletionRequest> = Result.failure(IllegalStateException("none"))
        var photo: PendingPhotoUpload? = null
        override fun observeProfile(): StateFlow<UserProfile?> = MutableStateFlow(null)
        override suspend fun refreshProfile(): Result<UserProfile> = error("unused")
        override suspend fun getMe(): Result<UserProfile> = error("unused")
        override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = Result.success(null)
        override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> { calls += "request"; return created }
        override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> { calls += "existing"; return existing }
        override suspend fun updateUsername(username: String): Result<Unit> { calls += "username:$username"; return Result.success(Unit) }
        override suspend fun updateAbout(about: String): Result<Unit> { calls += "about:$about"; return Result.failure(IllegalStateException("too long")) }
        override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> { calls += "avatar"; this.photo = photo; return Result.success(Unit) }
    }

    @Test fun editWritesGoThroughTheSharedRepositoryWithTheirOwnArguments() = runTest {
        val users = Users(); val port = AppProfileEditPort(users)
        assertTrue(port.updateAvatar(ProfileAvatarImage("a.png", "image/png", byteArrayOf(1, 2))).isSuccess)
        assertEquals(PendingPhotoUpload("a.png", "image/png", byteArrayOf(1, 2)), users.photo)
        assertTrue(port.updateUsername("Boris").isSuccess)
        assertEquals("too long", port.updateAbout("x").exceptionOrNull()?.message)
        assertEquals(listOf("avatar", "username:Boris", "about:x"), users.calls)
    }

    @Test fun deletionMapsTheRequestAndTheNoRequestCase() = runTest {
        val users = Users(); val port = AppAccountDeletionPort(users)
        assertNull(port.existingRequest().getOrThrow())
        users.existing = Result.success(AccountDeletionRequest("id", "pending", "2999", "2026-10-20T10:00:00Z"))
        assertEquals("2026-10-20T10:00:00Z", port.existingRequest().getOrThrow()?.resendAvailableAtUtc)
        users.created = Result.failure(IllegalStateException("boom"))
        assertEquals("boom", port.request().exceptionOrNull()?.message)
        users.created = Result.success(AccountDeletionRequest("id", "pending", "2999", "later"))
        assertEquals("later", port.request().getOrThrow().resendAvailableAtUtc)
        assertEquals(listOf("existing", "existing", "request", "request"), users.calls)
    }

    @Test fun sessionEndSignsOutOfThePlatformBeforeTheLoginRedirect() = runTest {
        val order = mutableListOf<String>()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val renderer = FeatureAccountScreenRenderer(
            edit = ThrowingEdit, deletion = ThrowingDeletion, appScope = scope,
            platformSignOut = { order += "signOut" }, openLogin = { order += "login" },
        )
        renderer.endSession()
        assertEquals(listOf("signOut", "login"), order)
    }

    private object ThrowingEdit : com.coffeepeek.feature.profile.api.EditProfileEntry {
        @androidx.compose.runtime.Composable
        override fun Content(onBack: () -> Unit, onOpenAccountDeletion: () -> Unit, modifier: androidx.compose.ui.Modifier) = error("unused")
    }

    private object ThrowingDeletion : com.coffeepeek.feature.profile.api.AccountDeletionEntry {
        @androidx.compose.runtime.Composable
        override fun Content(onClose: () -> Unit, onSessionEnded: () -> Unit, modifier: androidx.compose.ui.Modifier) = error("unused")
    }
}
