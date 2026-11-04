package com.coffeepeek.admin.settings

import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppLanguagePreferenceTest {
    private class ControlledSettings(
        private val stored: FakeSettingRepository, private val events: MutableList<String>,
    ) : SettingRepository by stored {
        var saveGate: CompletableDeferred<Unit>? = null
        var saveFailure: Exception? = null
        var readFailure: Exception? = null
        val attempts = mutableListOf<Setting>()

        override suspend fun save(model: Setting) {
            attempts += model
            saveGate?.await()
            saveFailure?.let { throw it }
            stored.save(model)
            events += "save:${model.value}"
        }

        override suspend fun read(key: String): Setting? {
            readFailure?.let { throw it }
            return stored.read(key)
        }
    }

    private class Fixture(scope: CoroutineScope, val stored: FakeSettingRepository = FakeSettingRepository()) {
        val events = mutableListOf<String>()
        val repository = ControlledSettings(stored, events)
        val applied = mutableListOf<String>()
        val preference = AppLanguagePreference(repository, scope) { tag ->
            applied += tag
            events += "apply:$tag"
        }
    }

    private fun TestScope.observe(fixture: Fixture) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        fixture.preference.observe().collect { fixture.events += "publish:${it.languageTag}" }
    }

    @Test
    fun missingAndUnsupportedSavedValuesRestoreRussianWithoutRewritingStorage() = runTest {
        listOf(null, "", "en", "BE", "be-BY").forEach { tag ->
            val values = tag?.let { mapOf(AppLanguagePreference.LANGUAGE_KEY to it) }.orEmpty()
            val fixture = Fixture(this, FakeSettingRepository(values))
            assertNull(fixture.preference.selection.value)
            runCurrent()
            assertEquals(AppLanguage.RUSSIAN, fixture.preference.current())
            assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
            assertEquals(listOf("ru"), fixture.applied)
            assertTrue(fixture.stored.saves.isEmpty())
        }
    }

    @Test
    fun aSavedBelarusianChoiceIsAppliedBeforeItIsPublished() = runTest {
        val fixture = Fixture(this, FakeSettingRepository(mapOf(AppLanguagePreference.LANGUAGE_KEY to "be")))
        observe(fixture)
        assertTrue(fixture.events.isEmpty())
        runCurrent()
        assertEquals(listOf("apply:be", "publish:be"), fixture.events)
        assertEquals(AppLanguage.BELARUSIAN, fixture.preference.selection.value)
        assertTrue(fixture.stored.saves.isEmpty())
    }

    @Test
    fun aSuccessfulChoiceSurvivesARecreatedPreference() = runTest {
        val first = Fixture(this)
        runCurrent()
        assertTrue(first.preference.set(AppLanguage.BELARUSIAN).isSuccess)
        val restarted = Fixture(this, first.stored)
        assertNull(restarted.preference.selection.value)
        runCurrent()
        assertEquals(AppLanguage.BELARUSIAN, restarted.preference.current())
        assertEquals(listOf("be"), restarted.applied)
        assertEquals(listOf(Setting(AppLanguagePreference.LANGUAGE_KEY, "be")), restarted.stored.saves)
    }

    @Test
    fun savingFinishesBeforePlatformApplicationAndPublication() = runTest {
        val fixture = Fixture(this)
        runCurrent()
        observe(fixture)
        fixture.events.clear()
        val gate = CompletableDeferred<Unit>()
        fixture.repository.saveGate = gate
        val change = async { fixture.preference.set(AppLanguage.BELARUSIAN) }
        runCurrent()
        assertFalse(change.isCompleted)
        assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
        assertNull(fixture.stored.valueOf(AppLanguagePreference.LANGUAGE_KEY))
        assertTrue(fixture.events.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertTrue(change.await().isSuccess)
        assertEquals(listOf("save:be", "apply:be", "publish:be"), fixture.events)
    }

    @Test
    fun aSaveFailureKeepsThePreviousChoiceAndCanBeRetried() = runTest {
        val fixture = Fixture(this)
        runCurrent()
        val error = IllegalStateException("storage unavailable")
        fixture.repository.saveFailure = error
        assertSame(error, fixture.preference.set(AppLanguage.BELARUSIAN).exceptionOrNull())
        assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
        assertEquals(listOf("ru"), fixture.applied)
        assertNull(fixture.stored.valueOf(AppLanguagePreference.LANGUAGE_KEY))
        fixture.repository.saveFailure = null
        assertTrue(fixture.preference.set(AppLanguage.BELARUSIAN).isSuccess)
        assertEquals(AppLanguage.BELARUSIAN, fixture.preference.selection.value)
        assertEquals("be", fixture.stored.valueOf(AppLanguagePreference.LANGUAGE_KEY))
        assertEquals(2, fixture.repository.attempts.size)
    }

    @Test
    fun cancellationDuringSaveIsRethrownAndReleasesTheMutex() = runTest {
        val fixture = Fixture(this)
        runCurrent()
        fixture.repository.saveGate = CompletableDeferred()
        val change = async { fixture.preference.set(AppLanguage.BELARUSIAN) }
        runCurrent()
        change.cancel(CancellationException("screen left"))
        runCurrent()
        assertFailsWith<CancellationException> { change.await() }
        assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
        assertEquals(listOf("ru"), fixture.applied)
        assertNull(fixture.stored.valueOf(AppLanguagePreference.LANGUAGE_KEY))
        fixture.repository.saveGate = null
        assertTrue(fixture.preference.set(AppLanguage.BELARUSIAN).isSuccess)
    }

    @Test
    fun aRepositoryCancellationIsNotReturnedAsAnOrdinaryFailure() = runTest {
        val fixture = Fixture(this)
        runCurrent()
        fixture.repository.saveFailure = CancellationException("save cancelled")
        assertFailsWith<CancellationException> { fixture.preference.set(AppLanguage.BELARUSIAN) }
        assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
        assertEquals(listOf("ru"), fixture.applied)
    }

    @Test
    fun concurrentChangesAreSerializedAndQueuedDuplicatesDoNotWriteAgain() = runTest {
        val fixture = Fixture(this)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        fixture.repository.saveGate = gate
        val first = async { fixture.preference.set(AppLanguage.BELARUSIAN) }
        val second = async { fixture.preference.set(AppLanguage.RUSSIAN) }
        val duplicate = async { fixture.preference.set(AppLanguage.RUSSIAN) }
        runCurrent()
        assertEquals(listOf("be"), fixture.repository.attempts.map { it.value })
        gate.complete(Unit)
        runCurrent()
        assertTrue(first.await().isSuccess && second.await().isSuccess && duplicate.await().isSuccess)
        assertEquals(listOf("be", "ru"), fixture.repository.attempts.map { it.value })
        assertEquals(listOf("ru", "be", "ru"), fixture.applied)
        assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
        assertEquals("ru", fixture.stored.valueOf(AppLanguagePreference.LANGUAGE_KEY))
    }

    @Test
    fun choosingTheCurrentLanguageDoesNotWriteOrReapplyIt() = runTest {
        val fixture = Fixture(this)
        runCurrent()
        assertTrue(fixture.preference.set(AppLanguage.RUSSIAN).isSuccess)
        assertTrue(fixture.repository.attempts.isEmpty())
        assertEquals(listOf("ru"), fixture.applied)
    }

    @Test
    fun aReadFailureRestoresRussianWithoutErasingTheSavedSetting() = runTest {
        val fixture = Fixture(this, FakeSettingRepository(mapOf(AppLanguagePreference.LANGUAGE_KEY to "be")))
        fixture.repository.readFailure = IllegalStateException("storage unavailable")
        runCurrent()
        assertEquals(AppLanguage.RUSSIAN, fixture.preference.selection.value)
        assertEquals("be", fixture.stored.valueOf(AppLanguagePreference.LANGUAGE_KEY))
        assertTrue(fixture.stored.saves.isEmpty())
    }
}
