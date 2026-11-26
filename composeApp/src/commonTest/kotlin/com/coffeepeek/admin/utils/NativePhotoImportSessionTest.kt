package com.coffeepeek.admin.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativePhotoImportSessionTest {
    @Test fun lateCameraCallbackCannotStartImportWhileTheParentStillObservesUi() = runTest {
        val state = MutableStateFlow(0)
        val observed = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect { observed += it } }
        val loading = mutableListOf<Boolean>()
        val results = mutableListOf<List<PickedImage>>()
        val picker = NativePhotoImportSession(backgroundScope, loading::add, results::add)
        var converted = 0
        picker.close()
        picker.start {
            converted++
            emptyList()
        }
        state.value = 1
        runCurrent()
        assertEquals(0, converted)
        assertEquals(listOf(0, 1), observed)
        assertTrue(loading.isEmpty())
        assertTrue(results.isEmpty())
    }

    @Test fun closeCancelsRunningImportWithoutReplacingTheExistingPhoto() = runTest {
        var cancelled = false
        val results = mutableListOf<List<PickedImage>>()
        val picker = NativePhotoImportSession(backgroundScope, {}, results::add)
        picker.start { try { awaitCancellation() } finally { cancelled = true } }
        runCurrent()
        picker.close()
        runCurrent()
        assertTrue(cancelled)
        assertFalse(picker.active)
        assertTrue(results.isEmpty())
    }

    @Test fun oldImportFinallyCannotClearBusyStateOfTheNextSelection() = runTest {
        val loading = mutableListOf<Boolean>()
        val picker = NativePhotoImportSession(backgroundScope, loading::add, {})
        val complete = CompletableDeferred<Unit>()
        picker.start { awaitCancellation() }
        runCurrent()
        picker.cancelSelection()
        picker.start {
            complete.await()
            emptyList()
        }
        runCurrent()
        assertEquals(listOf(true, false, true), loading)
        complete.complete(Unit)
        runCurrent()
        assertEquals(listOf(true, false, true, false), loading)
        picker.close()
    }

    @Test fun lateNonCancellablePlatformResultIsWipedInsteadOfDelivered() = runTest {
        val complete = CompletableDeferred<Unit>()
        val bytes = byteArrayOf(1, 2, 3)
        val results = mutableListOf<List<PickedImage>>()
        val picker = NativePhotoImportSession(backgroundScope, {}, results::add)
        picker.start { withContext(NonCancellable) {
            complete.await()
            listOf(PickedImage(bytes, "private-photo"))
        } }
        runCurrent()
        picker.close()
        complete.complete(Unit)
        runCurrent()
        assertTrue(results.isEmpty())
        assertContentEquals(byteArrayOf(0, 0, 0), bytes)
    }
}
