package com.coffeepeek.admin.feature.checkin.ui

import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoPreparation
import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoState

internal val unavailableDrinkPhotoPreparationFactory = DrinkPhotoPreparation.Factory { scope, changed ->
    DrinkPhotoPreparationController(scope, DrinkPhotoExtractor { error("Выделение чашки недоступно") }, changed)
}

internal class DrinkPhotoPreparationController(
    private val scope: CoroutineScope,
    private val extractor: DrinkPhotoExtractor,
    private val onChanged: (DrinkPhotoState) -> Unit,
) : DrinkPhotoPreparation {
    private val current = MutableStateFlow(DrinkPhotoState())
    private var job: Job? = null
    override val state: DrinkPhotoState get() = current.value

    override fun setSource(photo: PendingPhotoUpload?, selected: PendingPhotoUpload?, skipped: Boolean) {
        if (state.source == photo) return
        start(photo, selected, skipped)
    }

    override fun reset() = start(null, null, false)

    override fun retry() {
        val photo = state.source ?: return
        start(photo, null, false)
    }

    override fun skip() {
        job?.cancel()
        change { it.copy(selected = null, candidates = emptyList(), processing = false, skipped = true,
            error = null, revision = it.revision + 1) }
    }

    override fun select(photo: PendingPhotoUpload) {
        change { if (it.processing || photo !in it.candidates) it else it.copy(selected = photo, skipped = false) }
    }

    private fun start(photo: PendingPhotoUpload?, selected: PendingPhotoUpload?, skipped: Boolean) {
        job?.cancel()
        val next = change { previous -> DrinkPhotoState(
            source = photo, revision = previous.revision + 1,
            selected = selected.takeIf { photo != null },
            candidates = listOfNotNull(selected.takeIf { photo != null }),
            skipped = skipped && photo != null,
            processing = photo != null && selected == null && !skipped,
        ) }
        if (!next.processing || photo == null) return
        job = scope.launch {
            val result = try {
                val candidates = extractor.extract(photo)
                coroutineContext.ensureActive()
                check(candidates.isNotEmpty())
                next.copy(processing = false, candidates = candidates, selected = candidates.singleOrNull())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                next.copy(processing = false,
                    error = "Не удалось выделить чашку. Повторите попытку или сохраните без вырезки.")
            }
            if (current.compareAndSet(next, result)) onChanged(result)
        }
    }

    private fun change(update: (DrinkPhotoState) -> DrinkPhotoState): DrinkPhotoState {
        while (true) {
            val before = current.value
            val after = update(before)
            if (current.compareAndSet(before, after)) {
                onChanged(after)
                return after
            }
        }
    }
}
