package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInPhotoState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** One form-owned preparation. Uses the ViewModel's lifecycle scope, never an application/global scope. */
internal class HomeCheckInPhotoPreparation(
    private val scope: CoroutineScope,
    private val processor: HomeCheckInDrinkPhotoProcessor,
    private val onChanged: (HomeCheckInPhotoState) -> Unit,
) {
    var state = HomeCheckInPhotoState()
        private set
    private var job: Job? = null
    private var revision = 0L

    fun setSource(source: HomePreparationUpload?) {
        if (source != state.source) start(source)
    }

    fun retry() { if (!state.isProcessing) state.source?.let(::start) }

    fun skip() {
        cancel()
        publish(state.copy(selected = null, candidates = emptyList(), isProcessing = false, isSkipped = true, hasError = false))
    }

    fun select(index: Int) {
        if (state.isProcessing) return
        state.candidates.getOrNull(index)?.let { publish(state.copy(selected = it, isSkipped = false)) }
    }

    /** Clear without callbacks: the owner is already clearing its private state. */
    fun clear() {
        cancel()
        state = HomeCheckInPhotoState()
    }

    private fun cancel() {
        revision++
        job?.cancel()
        job = null
    }

    private fun publish(next: HomeCheckInPhotoState) {
        state = next
        onChanged(next)
    }

    private fun start(source: HomePreparationUpload?) {
        cancel()
        val expectedRevision = revision
        publish(HomeCheckInPhotoState(source = source, isProcessing = source != null))
        if (source == null) return
        val nextJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val candidates = processor.process(source).getOrThrow()
                coroutineContext.ensureActive()
                require(candidates.isNotEmpty() && candidates.all {
                    it.fileName.isNotBlank() && it.bytes.isNotEmpty() && it.contentType == "image/png"
                })
                if (revision == expectedRevision) {
                    val owned = candidates.map { it.copy(bytes = it.bytes.copyOf()) }
                    publish(state.copy(candidates = owned, selected = owned.singleOrNull(), isProcessing = false))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == expectedRevision) publish(state.copy(isProcessing = false, hasError = true))
            } finally {
                if (revision == expectedRevision) {
                    job = null
                    if (state.isProcessing) publish(state.copy(isProcessing = false))
                }
            }
        }
        job = nextJob
        nextJob.start()
    }
}
