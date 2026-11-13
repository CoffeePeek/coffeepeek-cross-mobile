package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.api.ShelfCoffeeOption
import com.coffeepeek.feature.shelf.api.ShelfCoffeeSearch
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.BagPhotoCropper
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.domain.usecase.SHELF_TEXT_LIMIT
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfScannerStatus.*
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val REVEAL_LIMIT_MILLIS = 2_500L

/**
 * One bag from photo to shelf. A new photo replaces the scan in progress; the form keeps what was typed across retakes.
 * The original photo stays in memory only; the processed card is what gets uploaded.
 */
internal class ShelfAddViewModel(
    private val repository: ShelfRepository,
    private val scanner: BagScanner,
    private val cropper: BagPhotoCropper,
    private val coffees: ShelfCoffeeSearch,
    private val arrivals: ShelfArrivals,
    calendar: ShelfCalendar,
    private val searchDelayMillis: Long = 300,
) : MviViewModel<ShelfAddState, ShelfAddAction, ShelfAddEvent>(ShelfAddState(today = calendar.today())) {
    private var scan: Job? = null
    private var search: Job? = null
    private var laterAfterConfirm = false
    private var beforeCamera: ShelfAddStep? = null
    private var beforeManualCrop: ShelfAddStep? = null
    private var prepareJob: Job? = null

    init {
        viewModelScope.launch {
            val readiness = try { scanner.readiness() } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { null }
            when (readiness) {
                is BagScannerReadiness.NeedsDownload -> updateState { copy(scanner = NeedsDownload(readiness.downloadBytes)) }
                BagScannerReadiness.Ready -> ready()
                null -> updateState { copy(scanner = DownloadFailed) }
            }
        }
    }

    override suspend fun handleActionInternal(action: ShelfAddAction) {
        when (action) {
            ShelfAddAction.DownloadScanner -> download()
            ShelfAddAction.OpenCamera -> openCamera()
            ShelfAddAction.CloseCamera -> closeCamera()
            ShelfAddAction.RevealFinished -> updateState { revealed() }
            ShelfAddAction.CardLanded -> updateState { copy(cardLanding = false) }
            is ShelfAddAction.PhotoPicked -> scan(action.photo)
            ShelfAddAction.PhotoFailed -> updateState { copy(message = ShelfAddMessage.PhotoUnreadable) }
            ShelfAddAction.StartManualCrop, ShelfAddAction.CancelManualCrop,
            is ShelfAddAction.CropChanged, is ShelfAddAction.ApplyManualCrop -> manualAction(action)
            is ShelfAddAction.QueryChanged -> queryChanged(action.query)
            is ShelfAddAction.CoffeeChosen -> coffeeChosen(action.option)
            is ShelfAddAction.RoasterChanged, is ShelfAddAction.WeightChosen, is ShelfAddAction.GrindChosen,
            is ShelfAddAction.RoastPurposeChosen, is ShelfAddAction.RoastDateChosen -> editForm(action)
            is ShelfAddAction.Save -> save(action.later, confirmed = false)
            ShelfAddAction.ConfirmDuplicate -> save(laterAfterConfirm, confirmed = true)
            ShelfAddAction.CancelDuplicate -> updateState { copy(confirmDuplicate = false) }
            ShelfAddAction.DismissMessage -> updateState { copy(message = null) }
            ShelfAddAction.Close -> sendEvent(ShelfAddEvent.Close)
        }
    }

    private suspend fun download() {
        val total = when (val status = currentState.scanner) {
            is NeedsDownload -> status.bytes
            DownloadFailed -> 0L
            else -> return
        }
        updateState { copy(scanner = Downloading(0, total)) }
        val job = currentCoroutineContext()[Job]
        prepareJob = job
        val result = try { scanner.prepare { done, all ->
            // A few hundred updates are enough for a progress bar; one per buffer would be thousands.
            updateState {
                val shown = scanner as? Downloading
                if (shown == null || (done - shown.done) * 200 < all && done != all) this else copy(scanner = Downloading(done, all))
            }
        } } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) { Result.failure(failure) }
        finally { if (prepareJob === job) prepareJob = null }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (result.isSuccess) ready() else updateState { copy(scanner = DownloadFailed) }
    }

    /** A prepared scanner resumes the exact photo chosen before preparation. */
    private fun ready() {
        updateState { copy(scanner = Ready) }
        currentState.photo?.takeIf { currentState.step == ShelfAddStep.Preparing }?.let(::scanReady)
    }

    private fun openCamera() {
        val state = currentState
        if (state.saving || state.step == ShelfAddStep.Camera) return
        beforeCamera = state.step.takeIf { it != ShelfAddStep.Scanning && it != ShelfAddStep.Revealing } ?: ShelfAddStep.Intro
        updateState { copy(step = ShelfAddStep.Camera) }
    }

    private suspend fun closeCamera() {
        if (currentState.step != ShelfAddStep.Camera) return
        val back = beforeCamera ?: ShelfAddStep.Intro
        beforeCamera = null
        updateState { copy(step = back) }
        if (back == ShelfAddStep.Preparing && currentState.scanner == Ready) currentState.photo?.let(::scanReady)
    }

    private fun scan(photo: ShelfPhoto) {
        if (currentState.saving) return
        scan?.cancel()
        updateState { copy(step = if (scanner == Ready) ShelfAddStep.Scanning else ShelfAddStep.Preparing,
            photo = photo, stagesDone = emptySet(), liveFrame = null, result = null, manualCrop = null,
            cropping = false, failure = null, cardLanding = false) }
        if (currentState.scanner == Ready) scanReady(photo)
    }

    private fun scanReady(photo: ShelfPhoto) {
        if (currentState.photo !== photo || currentState.step !in setOf(ShelfAddStep.Scanning, ShelfAddStep.Preparing)) return
        updateState { copy(step = ShelfAddStep.Scanning) }
        scan = viewModelScope.launch {
            val outcome = scanner.scan(photo.bytes,
                onStage = { stage -> updateState { if (this.photo === photo) copy(stagesDone = stagesDone + stage) else this } },
                onFront = { frame -> updateState { if (this.photo === photo) copy(liveFrame = frame) else this } })
            outcome.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            updateState { if (this.photo === photo) withOutcome(outcome) else this }
            // The peel animation ends the reveal; should it never finish (an undecodable card), the form opens anyway.
            delay(REVEAL_LIMIT_MILLIS)
            updateState { if (this.photo === photo) revealed() else this }
        }
    }

    private suspend fun startManualCrop() {
        val state = currentState
        if (state.photo == null || state.saving || state.step !in setOf(ShelfAddStep.Preparing, ShelfAddStep.Retake)) return
        scan?.cancel()
        beforeManualCrop = state.step
        updateState { copy(step = ShelfAddStep.ManualCrop, cropBounds = BagCropBounds.Default, cropping = false) }
        val preparing = prepareJob
        if (preparing != null) {
            preparing.cancelAndJoin()
            refreshScannerReadiness()
        }
    }

    private suspend fun refreshScannerReadiness() {
        val readiness = try { scanner.readiness() } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }
        updateState {
            copy(scanner = when (readiness) {
                BagScannerReadiness.Ready -> Ready
                is BagScannerReadiness.NeedsDownload -> NeedsDownload(readiness.downloadBytes)
                null -> DownloadFailed
            })
        }
    }

    private suspend fun manualAction(action: ShelfAddAction) {
        when (action) {
            ShelfAddAction.StartManualCrop -> startManualCrop()
            ShelfAddAction.CancelManualCrop -> cancelManualCrop()
            is ShelfAddAction.CropChanged -> updateState {
                if (step == ShelfAddStep.ManualCrop && action.bounds.isValid()) copy(cropBounds = action.bounds) else this
            }
            is ShelfAddAction.ApplyManualCrop -> applyManualCrop(action.bounds)
            else -> Unit
        }
    }

    private fun cancelManualCrop() {
        if (currentState.step != ShelfAddStep.ManualCrop) return
        val back = beforeManualCrop ?: ShelfAddStep.Preparing
        beforeManualCrop = null
        updateState { copy(step = back) }
        if (back == ShelfAddStep.Preparing && currentState.scanner == Ready) currentState.photo?.let(::scanReady)
    }

    private suspend fun applyManualCrop(bounds: BagCropBounds) {
        val state = currentState
        val photo = state.photo ?: return
        if (state.step != ShelfAddStep.ManualCrop || state.cropping || !bounds.isValid()) return
        updateState { copy(cropping = true, cropBounds = bounds) }
        val result = cropper.crop(photo.bytes, bounds)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        updateState {
            if (this.photo !== photo || step != ShelfAddStep.ManualCrop) this
            else result.fold({ copy(manualCrop = it, result = null, failure = null, cropping = false, step = ShelfAddStep.Form) },
                { copy(cropping = false, message = ShelfAddMessage.PhotoUnreadable) })
        }
    }

    private fun ShelfAddState.revealed() = if (step == ShelfAddStep.Revealing) copy(step = ShelfAddStep.Form, cardLanding = true) else this

    private fun ShelfAddState.withOutcome(outcome: Result<BagScanResult>): ShelfAddState = outcome.fold(
        { copy(result = it, step = if (it.card == null || it.moderation.status == PackModerationStatus.Retake) ShelfAddStep.Retake else ShelfAddStep.Revealing) },
        { copy(failure = (it as? BagScanFailure)?.issue ?: BagScanIssue.Failed, step = ShelfAddStep.Retake) },
    )

    private fun queryChanged(query: String) {
        val limited = query.take(SHELF_TEXT_LIMIT)
        updateState { copy(form = form.copy(query = limited, coffee = null), options = emptyList()) }
        search?.cancel()
        if (limited.trim().length < 2) return
        search = viewModelScope.launch {
            delay(searchDelayMillis)
            coffees.search(limited.trim()).onSuccess { found ->
                updateState { if (form.query == limited && form.coffee == null) copy(options = found.take(5)) else this }
            }
        }
    }

    private fun coffeeChosen(option: ShelfCoffeeOption) {
        search?.cancel()
        updateState { copy(form = form.copy(query = option.name, coffee = option, roaster = option.roasterName), options = emptyList()) }
    }

    private suspend fun save(later: Boolean, confirmed: Boolean) {
        val state = currentState
        val card = state.card ?: return
        if (state.saving) return
        val details = if (later) ShelfBagDetails() else state.form.details()
        val slug = (details.bean as? ShelfBean.Catalog)?.slug
        updateState { copy(saving = true, confirmDuplicate = false) }
        if (!confirmed && slug != null && repository.shelf.first()?.any { it.coffeeProductSlug == slug } == true) {
            laterAfterConfirm = later
            updateState { copy(saving = false, confirmDuplicate = true) }
            return
        }
        val saved = repository.add(details, card)
        saved.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        saved
            .onSuccess { bag ->
                arrivals.arrived(bag.id)
                updateState { copy(saving = false) }
                sendEvent(ShelfAddEvent.Saved)
            }
            .onFailure { error -> updateState { copy(saving = false, message = ShelfAddMessage.Failed(issueOf(error))) } }
    }

    /** Plain field edits. The picker offers no future roast dates; one that slips through is dropped, as the server would refuse it. */
    private fun editForm(action: ShelfAddAction) {
        val today = currentState.today
        when (action) {
            is ShelfAddAction.RoasterChanged -> updateForm { copy(roaster = action.roaster.take(SHELF_TEXT_LIMIT)) }
            is ShelfAddAction.WeightChosen -> updateForm { copy(weightGrams = action.grams) }
            is ShelfAddAction.GrindChosen -> updateForm { copy(grind = action.grind) }
            is ShelfAddAction.RoastPurposeChosen -> updateForm { copy(roastPurpose = action.purpose) }
            is ShelfAddAction.RoastDateChosen -> updateForm { copy(roastedOn = action.day?.takeIf { it.epochDay <= today.epochDay }) }
            else -> Unit
        }
    }

    private fun updateForm(change: ShelfBagForm.() -> ShelfBagForm) = updateState { copy(form = form.change()) }
}
