package com.coffeepeek.admin.feature.checkin

import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoPreparation
import com.coffeepeek.admin.feature.checkin.ui.DrinkPhotoPreparationController
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DrinkPhotoPreparationTest {
    private val first = photo("first.jpg", 1)
    private val second = photo("second.jpg", 2)
    private val cutout = photo("cup.png", 3)

    @Test
    fun staleNativeCompletionCannotReplaceTheNextFirstPhotoOrUndoSkip() = runTest {
        val finishOld = CompletableDeferred<Unit>()
        val preparation = DrinkPhotoPreparationController(this, DrinkPhotoExtractor { original ->
            if (original == first) withContext(NonCancellable) { finishOld.await() }
            listOf(if (original == first) photo("stale.png", 9) else cutout)
        }) {}
        preparation.setSource(first)
        assertFalse(preparation.state.canSubmit)
        runCurrent()
        preparation.setSource(second)
        runCurrent()
        assertEquals(cutout, preparation.state.selected)
        preparation.skip()
        finishOld.complete(Unit)
        runCurrent()
        assertTrue(preparation.state.skipped)
        assertNull(preparation.state.selected)
        assertTrue(preparation.state.canSubmit)
    }

    @Test
    fun multipleObjectsRequireSelectionAndChangingOrRemovingFirstInvalidatesIt() = runTest {
        val other = photo("other.png", 4)
        var attempts = 0
        val preparation = DrinkPhotoPreparationController(this, DrinkPhotoExtractor { attempts++; listOf(cutout, other) }) {}
        preparation.setSource(first)
        runCurrent()
        assertFalse(preparation.state.canSubmit)
        preparation.select(other)
        assertEquals(other, preparation.state.selected)
        assertTrue(preparation.state.canSubmit)
        preparation.setSource(first.copy(bytes = first.bytes.copyOf()))
        runCurrent()
        assertEquals(1, attempts, "Unchanged source content must not restart segmentation")
        preparation.setSource(second)
        assertNull(preparation.state.selected)
        assertTrue(preparation.state.processing)
        preparation.setSource(null)
        runCurrent()
        assertTrue(preparation.state.canSubmit)
        assertTrue(preparation.state.candidates.isEmpty())
    }

    @Test
    fun extractionFailureRetainsSourceAllowsSavingWithoutCutoutAndRetryRecovers() = runTest {
        var attempts = 0
        val preparation = DrinkPhotoPreparationController(this, DrinkPhotoExtractor {
            if (attempts++ == 0) error("Native model unavailable")
            listOf(cutout)
        }) {}
        preparation.setSource(first)
        runCurrent()
        assertNotNull(preparation.state.error)
        assertTrue(preparation.state.canSubmit)
        assertNull(preparation.state.selected)
        preparation.retry()
        runCurrent()
        assertEquals(cutout, preparation.state.selected)
        assertNull(preparation.state.error)
        assertContentEquals(byteArrayOf(1), first.bytes)
    }

    private fun photo(name: String, byte: Int) = PendingPhotoUpload(name,
        if (name.endsWith("png")) "image/png" else "image/jpeg", byteArrayOf(byte.toByte()))
}
