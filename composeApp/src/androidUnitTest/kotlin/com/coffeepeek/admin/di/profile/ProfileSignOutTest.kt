package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import com.coffeepeek.feature.notifications.api.NotificationFeature
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileSignOutTest {
    private val events = mutableListOf<String>()

    @AfterTest fun tearDown() = ErrorHandler.clearError()

    private fun signOut(logout: Result<Unit>) = ProfileSignOut(
        notifications = Proxy.newProxyInstance(NotificationFeature::class.java.classLoader, arrayOf(NotificationFeature::class.java)) { _, method, _ ->
            if (method.name == "beforeLogout") events += "push"; null
        } as NotificationFeature,
        auth = object : AuthRepository {
            override suspend fun login(email: String, password: String): Result<Session> = error("unused")
            override suspend fun googleLogin(idToken: String): Result<Session> = error("unused")
            override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> = error("unused")
            override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> = error("unused")
            override suspend fun register(userName: String, email: String, password: String): Result<Unit> = error("unused")
            override suspend fun isEmailTaken(email: String): Result<Boolean> = error("unused")
            override suspend fun logout(): Result<Unit> { events += "server"; return logout }
        },
        drafts = ReviewDraftStore(object : SettingRepository {
            override suspend fun save(model: Setting) = Unit
            override suspend fun read(key: String): Setting? = null
            override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
            override suspend fun readAll(): List<Setting> { events += "drafts"; return emptyList() }
            override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
            override suspend fun delete(key: String) = Unit
        }),
        platformSignOut = { events += "platform" },
    )

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
}
