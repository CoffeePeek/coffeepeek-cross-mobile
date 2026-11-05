package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.di.iosPlatformModules
import com.coffeepeek.admin.ui.screen.profile.PersonalProfileWeekCache
import com.coffeepeek.admin.ui.screen.profile.PersonalProfileWeekSnapshot
import com.coffeepeek.core.session.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.error.NoDefinitionFoundException
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
class IosProfileCompositionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun prepareMain() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun resetMain() { Dispatchers.resetMain() }

    @Test fun missingProfileModuleReproducesTheReleaseDependencyFailure() = runTest(dispatcher) {
        val oldModules = iosPlatformModules(BASE_URL).filterNot { candidate ->
            candidate.mappings.values.any { it.beanDefinition.primaryType == PersonalProfileWeekCache::class }
        }
        withProfileGraph(oldModules) { koin, _ ->
            assertFailsWith<NoDefinitionFoundException> { koin.get<PersonalProfileWeekCache>() }
        }
    }

    @Test fun theProductionPlatformGraphProvidesTheProfileDependenciesAsSingletons() = runTest(dispatcher) {
        withProfileGraph { koin, sessions ->
            assertSame(koin.get<PersonalProfileWeekCache>(), koin.get<PersonalProfileWeekCache>())
            assertSame(koin.get<ProfileSnapshotStore>(), koin.get<ProfileSnapshotStore>())
            runCurrent()
            assertEquals(1, sessions.observers)
        }
    }

    @Test fun profileCacheIsClearedAcrossLogoutAndSameOwnerRelogin() = runTest(dispatcher) {
        withProfileGraph { koin, sessions ->
            val cache = koin.get<PersonalProfileWeekCache>()
            val week = PersonalProfileWeekSnapshot(mapOf("2026-11-05" to 2), emptyMap())
            cache.write("1:owner", "today", week)
            assertEquals(week, cache.read("1:owner", "today"))
            sessions.applySession(null)
            runCurrent()
            assertNull(cache.read("1:owner", "today"))
            sessions.applySession(Session("fixture-relogin", userId = "owner"))
            runCurrent()
            assertNull(cache.read("3:owner", "today"))
            cache.write("3:owner", "today", week)
            assertEquals(week, cache.read("3:owner", "today"))
        }
    }

    private fun TestScope.withProfileGraph(platformModules: List<Module> = iosPlatformModules(BASE_URL),
        block: TestScope.(Koin, Sessions) -> Unit) {
        val sessions = Sessions()
        val app = koinApplication {
            modules(platformModules + module { single<SessionRepository> { sessions } })
        }
        try { block(app.koin, sessions) }
        finally { app.close() }
        runCurrent()
        assertEquals(0, sessions.observers)
    }

    private class Sessions : SessionRepository {
        private val state = MutableStateFlow<Session?>(Session("fixture", userId = "owner"))
        private var generation = 1L
        var observers = 0
        override fun accountGeneration() = generation
        override fun peekSession() = state.value
        override fun isActiveSession(session: Session?) = !session?.userId.isNullOrBlank()
        override fun observeSession() = state.onStart { observers++ }.onCompletion { observers-- }
        override fun applySession(session: Session?) {
            if (state.value?.userId != session?.userId || state.value == null) generation++
            state.value = session
        }
        override suspend fun getSession(): Session? = error("Graph resolution must not load credentials")
        override suspend fun persistSession(session: Session?) = error("Unexpected credential write")
        override suspend fun saveSession(session: Session?) = error("Unexpected credential write")
        override suspend fun warmCache() = error("Unexpected credential load")
        override suspend fun isLoggedIn(): Boolean = error("Unexpected session load")
    }

    private companion object { const val BASE_URL = "https://fixture.invalid" }
}
