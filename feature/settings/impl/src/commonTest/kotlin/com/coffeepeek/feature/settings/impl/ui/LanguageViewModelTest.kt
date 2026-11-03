package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.settings.impl.ui.model.LanguageAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private class SavedLanguage(var value: AppLanguage = AppLanguage.RUSSIAN)

    private class FakeLanguage(private val saved: SavedLanguage = SavedLanguage()) : LanguagePort {
        val selected = MutableStateFlow(saved.value)
        val writes = mutableListOf<AppLanguage>()
        var result: Result<Unit> = Result.success(Unit)
        var failure: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        override fun current() = selected.value
        override fun observe(): Flow<AppLanguage> = selected
        override suspend fun set(language: AppLanguage): Result<Unit> {
            writes += language
            gate?.await()
            failure?.let { throw it }
            return result.onSuccess {
                saved.value = language
                selected.value = language
            }
        }
    }

    private fun model(port: FakeLanguage) = LanguageViewModel(port).also { store.put("language", it) }

    @Test
    fun startsWithTheRestoredChoiceBeforeTheFlowEmits() = runTest(dispatcher) {
        val vm = model(FakeLanguage(SavedLanguage(AppLanguage.BELARUSIAN)))
        assertEquals(AppLanguage.BELARUSIAN, vm.state.value.selected)
    }

    @Test
    fun aChoiceIsAppliedAndSavedThroughTheInjectedPort() = runTest(dispatcher) {
        val saved = SavedLanguage()
        val port = FakeLanguage(saved)
        val vm = model(port)
        runCurrent()
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertEquals(listOf(AppLanguage.BELARUSIAN), port.writes)
        assertEquals(AppLanguage.BELARUSIAN, saved.value)
        assertEquals(AppLanguage.BELARUSIAN, vm.state.value.selected)
    }

    @Test
    fun repeatedTapsDoNotWriteTheSameLanguageAgain() = runTest(dispatcher) {
        val port = FakeLanguage()
        val vm = model(port)
        vm.onAction(LanguageAction.Select(AppLanguage.RUSSIAN))
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertEquals(listOf(AppLanguage.BELARUSIAN), port.writes)
    }

    @Test
    fun savingKeepsTheOldSelectionAndIgnoresConcurrentChoices() = runTest(dispatcher) {
        val port = FakeLanguage().apply { gate = CompletableDeferred() }
        val vm = model(port)
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertTrue(vm.state.value.isSaving)
        assertEquals(AppLanguage.RUSSIAN, vm.state.value.selected)
        vm.onAction(LanguageAction.Select(AppLanguage.RUSSIAN))
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        vm.onAction(LanguageAction.RetrySave)
        runCurrent()
        assertEquals(listOf(AppLanguage.BELARUSIAN), port.writes)
        port.gate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.isSaving)
        assertEquals(AppLanguage.BELARUSIAN, vm.state.value.selected)
    }

    @Test
    fun aFailedSavePreservesTheOldLanguageAndRetrySavesTheSameChoice() = runTest(dispatcher) {
        val saved = SavedLanguage()
        val port = FakeLanguage(saved).apply { result = Result.failure(IllegalStateException("storage unavailable")) }
        val vm = model(port)
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertEquals(AppLanguage.RUSSIAN, vm.state.value.selected)
        assertEquals(AppLanguage.RUSSIAN, saved.value)
        assertTrue(vm.state.value.hasSaveError)
        assertFalse(vm.state.value.isSaving)
        port.result = Result.success(Unit)
        vm.onAction(LanguageAction.RetrySave)
        runCurrent()
        assertEquals(listOf(AppLanguage.BELARUSIAN, AppLanguage.BELARUSIAN), port.writes)
        assertEquals(AppLanguage.BELARUSIAN, saved.value)
        assertEquals(AppLanguage.BELARUSIAN, vm.state.value.selected)
        assertFalse(vm.state.value.hasSaveError)
    }

    @Test
    fun choosingTheCurrentLanguageClearsAFailedChoiceWithoutSaving() = runTest(dispatcher) {
        val port = FakeLanguage().apply { result = Result.failure(IllegalStateException("offline")) }
        val vm = model(port)
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        vm.onAction(LanguageAction.Select(AppLanguage.RUSSIAN))
        runCurrent()
        assertEquals(listOf(AppLanguage.BELARUSIAN), port.writes)
        assertFalse(vm.state.value.hasSaveError)
    }

    @Test
    fun aThrowingStoragePortPreservesTheOldLanguageAndOffersRetry() = runTest(dispatcher) {
        val port = FakeLanguage().apply { failure = IllegalStateException("storage unavailable") }
        val vm = model(port)
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertEquals(AppLanguage.RUSSIAN, vm.state.value.selected)
        assertTrue(vm.state.value.hasSaveError)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun thrownCancellationDoesNotBecomeASaveErrorAndReleasesTheGuard() = checkCancellation(false)

    @Test
    fun resultCancellationDoesNotBecomeASaveErrorAndReleasesTheGuard() = checkCancellation(true)

    private fun checkCancellation(wrapped: Boolean) = runTest(dispatcher) {
        val cancelled = CancellationException("screen left")
        val port = FakeLanguage().apply {
            if (wrapped) result = Result.failure(cancelled) else failure = cancelled
        }
        val vm = model(port)
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertEquals(AppLanguage.RUSSIAN, vm.state.value.selected)
        assertFalse(vm.state.value.hasSaveError)
        assertFalse(vm.state.value.isSaving)
        port.result = Result.success(Unit)
        port.failure = null
        vm.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        assertEquals(AppLanguage.BELARUSIAN, vm.state.value.selected)
    }

    @Test
    fun followsChangesMadeElsewhereWithoutWritingThemBack() = runTest(dispatcher) {
        val port = FakeLanguage()
        val vm = model(port)
        runCurrent()
        port.selected.value = AppLanguage.BELARUSIAN
        runCurrent()
        assertEquals(AppLanguage.BELARUSIAN, vm.state.value.selected)
        assertEquals(emptyList(), port.writes)
    }

    @Test
    fun aRecreatedScreenReadsThePreviouslyChosenLanguageFromThePort() = runTest(dispatcher) {
        val saved = SavedLanguage()
        val first = model(FakeLanguage(saved))
        first.onAction(LanguageAction.Select(AppLanguage.BELARUSIAN))
        runCurrent()
        store.clear()
        val recreated = model(FakeLanguage(saved))
        assertEquals(AppLanguage.BELARUSIAN, recreated.state.value.selected)
    }

    @Test
    fun clearingTheScreenStopsFollowingThePort() = runTest(dispatcher) {
        val port = FakeLanguage()
        val vm = model(port)
        runCurrent()
        store.clear()
        port.selected.value = AppLanguage.BELARUSIAN
        runCurrent()
        assertEquals(AppLanguage.RUSSIAN, vm.state.value.selected)
    }
}
