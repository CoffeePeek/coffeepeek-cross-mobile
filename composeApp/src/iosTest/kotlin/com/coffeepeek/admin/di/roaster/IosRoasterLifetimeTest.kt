package com.coffeepeek.admin.di.roaster

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosRoasterLifetimeTest {
    private val dispatcher = StandardTestDispatcher()
    private val accounts = Accounts()
    private val language = object : LanguagePort {
        override fun current() = AppLanguage.RUSSIAN
        override fun observe() = flowOf(current())
        override suspend fun set(language: AppLanguage) = Result.success(Unit)
    }
    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() = Dispatchers.resetMain()

    @Test fun credentialsRefreshPreservesTheHostButAccountReplacementRevokesItPermanently() = runTest(dispatcher) {
        val source = Probe(language, accounts)
        val states = mutableListOf<Boolean>()
        val observation = source.observeCurrent(states::add)
        runCurrent()
        accounts.session.value = Session("refreshed", userId = "a")
        runCurrent()
        assertTrue(source.isCurrent())
        accounts.replace("b")
        runCurrent()
        assertFalse(source.isCurrent())
        assertEquals(false, states.last())
        accounts.replace("a")
        assertFalse(source.isCurrent())
        observation.cancel()
        source.close()
    }

    @Test fun closingCancelsImageStreamsAndDisablesRetainedNativeActions() = runTest(dispatcher) {
        var imageCancelled = false
        val source = Probe(language, accounts) { imageCancelled = true }
        var frames = 0
        source.loadImage("fixture") { frames++ }
        runCurrent()
        assertEquals(1, frames)
        source.close()
        source.act()
        runCurrent()
        assertTrue(imageCancelled)
        assertFalse(source.acted)
    }

    private class Accounts : SessionReader {
        val session = MutableStateFlow<Session?>(Session("fixture", userId = "a"))
        private var generation = 0L
        override fun accountGeneration() = generation
        override fun peekSession() = session.value
        override fun isActiveSession(session: Session?) = session != null && session == this.session.value
        override fun observeSession() = session
        override suspend fun getSession() = session.value
        fun replace(id: String) {
            generation++
            session.value = Session("fixture-$generation", userId = id)
        }
    }
    private class Probe(language: LanguagePort, accounts: SessionReader, onImageCancelled: () -> Unit = {}) :
        IosRoasterScreen(language, accounts, { _, _ -> flow {
            try { emit(null); awaitCancellation() } finally { onImageCancelled() }
        } }) {
        var acted = false
        fun act() = guarded { acted = true }
        fun observeCurrent(receive: (Boolean) -> Unit) = observe(flowOf(Unit)) { receive(isCurrent()) }
    }
}
