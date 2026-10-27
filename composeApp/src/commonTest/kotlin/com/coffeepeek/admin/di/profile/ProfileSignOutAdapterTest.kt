package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.feature.auth.domain.AppleSignInChallenge
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.SignedInAccount
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Fake ports only: no platform provider, push service, database or account writes. */
class ProfileSignOutAdapterTest {
    private val events = mutableListOf<String>()
    @BeforeTest fun before() = ErrorHandler.clearError()
    @AfterTest fun after() = ErrorHandler.clearError()

    private fun signOut(logout: Result<Unit>) = AppProfileSignOut(
        notifications = notifications(), auth = auth(logout), drafts = drafts(),
        platformSignOut = { events += "platform" },
    )

    private fun notifications() = object : NotificationFeature {
        override val state = MutableStateFlow(NotificationInboxState())
        override fun refresh() = Unit
        override fun loadMore() = Unit
        override fun setPushEnabled(enabled: Boolean) = Unit
        override fun markRead(id: String) = Unit
        override fun markAllRead() = Unit
        override suspend fun destination(notificationId: String): com.coffeepeek.feature.notifications.api.NotificationDestination? =
            error("Sign-out must not resolve notification destinations")
        override fun foreground() = Unit
        override fun onCheckInCreated() = Unit
        override suspend fun beforeLogout() { events += "push" }
    }

    private fun auth(logout: Result<Unit>) = object : AuthRepository {
        override suspend fun signIn(email: String, password: String): Result<SignedInAccount> = error("unused")
        override suspend fun signInWithGoogle(idToken: String): Result<SignedInAccount> = error("unused")
        override suspend fun beginAppleSignIn(): Result<AppleSignInChallenge> = error("unused")
        override suspend fun signInWithApple(challengeId: String, idToken: String): Result<SignedInAccount> = error("unused")
        override suspend fun register(userName: String, email: String, password: String): Result<Unit> = error("unused")
        override suspend fun isEmailTaken(email: String): Result<Boolean> = error("unused")
        override suspend fun requestPasswordReset(email: String): Result<Unit> = error("unused")
        override suspend fun signOut(): Result<Unit> {
            events += "server"
            return logout
        }
    }

    private fun drafts() = ReviewDraftStore(object : SettingRepository {
        override suspend fun save(model: Setting) = Unit
        override suspend fun read(key: String): Setting? = null
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll(): List<Setting> {
            events += "drafts"
            return emptyList()
        }
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) = Unit
    })

    @Test fun signsOutOnTheServerThenThePlatformThenClearsDrafts() = runTest {
        signOut(Result.success(Unit)).signOut()
        assertEquals(listOf("push", "server", "platform", "drafts"), events)
        assertNull(ErrorHandler.errorMessage.value)
    }

    @Test fun aServerFailureIsShownAndTheLocalCleanupStillRuns() = runTest {
        signOut(Result.failure(RuntimeException("server said no"))).signOut()
        assertEquals(listOf("push", "server", "platform", "drafts"), events)
        assertEquals("server said no", ErrorHandler.errorMessage.value)
    }

    @Test fun cancellationIsNotAnErrorMessageOrPermissionToContinueCleanup() = runTest {
        assertFailsWith<CancellationException> { signOut(Result.failure(CancellationException())).signOut() }
        assertEquals(listOf("push", "server"), events)
        assertNull(ErrorHandler.errorMessage.value)
    }
}
