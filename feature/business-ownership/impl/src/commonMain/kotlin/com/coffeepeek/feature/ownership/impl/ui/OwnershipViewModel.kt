package com.coffeepeek.feature.ownership.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.coroutineContext

internal class OwnershipViewModel(
    private val repository: BusinessOwnershipRepository,
    private val account: OwnershipAccount,
    private val initialKind: BusinessKind? = null,
    private val initialSlug: String? = null,
    private val initialId: String? = null,
    private val businessTab: Boolean = false,
) : MviViewModel<OwnershipState, OwnershipAction, OwnershipEvent>(OwnershipState()) {
    private val operations = mutableSetOf<Job>()
    private var cardsJob: Job? = null
    private var initialized = false
    init {
        viewModelScope.launch {
            account.observe().distinctUntilChanged().collect { identity ->
                cardsJob?.cancel()
                operations.toList().forEach { it.cancel() }
                operations.clear()
                val generation = currentState.generation + 1
                updateState { OwnershipState(account = identity, generation = generation) }
                if (identity != null) initialize()
            }
        }
    }
    private fun initialize() {
        if (!initialized && initialKind != null && initialId != null) {
            onAction(OwnershipAction.OpenBusiness(initialKind, initialId))
        } else if (!initialized && initialKind != null && !initialSlug.isNullOrBlank()) {
            updateState { copy(destination = OwnershipDestination.Submit, submission = ClaimSubmission(initialKind, initialSlug, "", "")) }
        } else onAction(OwnershipAction.Refresh)
        initialized = true
    }
    override suspend fun handleActionInternal(action: OwnershipAction) {
        if (action == OwnershipAction.Login) return sendEvent(OwnershipEvent.Login)
        if (action == OwnershipAction.Back) return back()
        if (account.current() == null || currentState.busy || currentState.loading) return
        val job = coroutineContext.job
        operations.add(job)
        try { dispatch(action) } finally { operations.remove(job) }
    }
    private suspend fun dispatch(action: OwnershipAction) {
        when (action) {
            OwnershipAction.Refresh -> refresh()
            OwnershipAction.NewClaim -> updateState { copy(destination = OwnershipDestination.Submit, problem = null, selectedClaim = null) }
            OwnershipAction.Submit -> submit()
            OwnershipAction.AskSave -> askSave()
            OwnershipAction.Save -> save()
            OwnershipAction.AskReload -> updateState { copy(confirmDiscard = true, reloadAfterDiscard = true) }
            OwnershipAction.Reload -> reload()
            OwnershipAction.Discard -> if (initialKind != null) sendEvent(OwnershipEvent.Close) else overview()
            OwnershipAction.CancelConfirmation -> updateState { copy(confirmDiscard = false, confirmPublish = false) }
            is OwnershipAction.OpenClaim -> openClaim(action.id)
            is OwnershipAction.OpenBusiness -> openProfile(action.kind, action.id)
            is OwnershipAction.ClaimsPage -> changeClaimsPage(action.page)
            is OwnershipAction.RoastersPage -> changeRoastersPage(action.page)
            else -> edit(action)
        }
    }
    private suspend fun changeClaimsPage(page: Int) {
        updateState { copy(claimsPage = maxOf(1, page)) }
        overview()
    }
    private suspend fun changeRoastersPage(page: Int) {
        updateState { copy(roastersPage = maxOf(1, page)) }
        overview()
    }
    private fun edit(action: OwnershipAction) = updateState {
        when (action) {
            is OwnershipAction.Kind -> copy(submission = submission.copy(kind = action.kind), problem = null)
            is OwnershipAction.ClaimText -> copy(submission = submission.edit(action.field, action.value), problem = null)
            is OwnershipAction.ProfileText -> copy(draft = draft?.edit(action.field, action.value), saved = false, problem = if (reloadRequired) problem else null)
            is OwnershipAction.AddEvidence -> if (action.generation == generation) copy(submission = submission.copy(photos = (submission.photos + action.photos).take(3)), problem = null) else this
            is OwnershipAction.MoveEvidence -> copy(submission = submission.copy(photos = submission.photos.move(action.from, action.to)))
            is OwnershipAction.RemoveEvidence -> copy(submission = submission.copy(photos = submission.photos.filterIndexed { index, _ -> index != action.index }))
            is OwnershipAction.AddPhotos -> if (action.generation == generation) copy(draft = draft?.copy(photos = (draft.photos + action.photos).take(10)), saved = false) else this
            is OwnershipAction.RemovePhoto -> copy(draft = draft?.copy(photos = draft.photos.filterIndexed { index, _ -> index != action.index }), saved = false)
            is OwnershipAction.MovePhoto -> copy(draft = draft?.copy(photos = draft.photos.move(action.from, action.to)), saved = false)
            else -> this
        }
    }
    private suspend fun overview() {
        cardsJob?.cancel()
        updateState { copy(destination = OwnershipDestination.Overview, selectedClaim = null, profile = null, draft = null, loading = true, problem = null, confirmDiscard = false) }
        val snapshot = currentState
        try {
            val data = fetchOverview(snapshot)
            currentCoroutineContext().ensureActive()
            if (snapshot.generation != currentState.generation) return
            val businesses = data.second + data.third.items
            val identities = businesses.map { it.kind to it.id }.toSet()
            updateState { copy(claims = data.first, shops = data.second, roasters = data.third,
                cardProfiles = cardProfiles.filterKeys { it in identities }, loading = false) }
            if (businessTab) loadCardProfiles(snapshot.generation, businesses)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { failed(failure) }
    }
    private fun loadCardProfiles(generation: Int, businesses: List<OwnedBusiness>) {
        cardsJob = viewModelScope.launch {
            val permits = Semaphore(4)
            businesses.forEach { business ->
                launch {
                    val result = permits.withPermit { repository.profile(business.kind, business.id) }
                    currentCoroutineContext().ensureActive()
                    result.onSuccess { profile ->
                        updateState { if (this.generation == generation) copy(cardProfiles = cardProfiles + ((business.kind to business.id) to profile)) else this }
                    }.onFailure { if (it is CancellationException) throw it }
                }
            }
        }
    }
    private suspend fun fetchOverview(snapshot: OwnershipState) = coroutineScope {
        val claims = async { if (businessTab) Result.success(BusinessPage<OwnershipClaim>(emptyList(), 0, 1, 20)) else repository.claims(snapshot.claimsPage) }
        val shops = async { repository.businesses(BusinessKind.CoffeeShop, 1) }
        val roasters = async { repository.businesses(BusinessKind.Roaster, snapshot.roastersPage) }
        Triple(claims.await().getOrThrow(), shops.await().getOrThrow().items, roasters.await().getOrThrow())
    }

    private suspend fun refresh() {
        when (currentState.destination) {
            OwnershipDestination.Overview -> overview()
            OwnershipDestination.Claim -> currentState.selectedClaimId?.let { openClaim(it) }
            OwnershipDestination.Editor -> if (currentState.dirty) updateState { copy(confirmDiscard = true) } else reload()
            OwnershipDestination.Submit -> Unit
        }
    }
    private suspend fun openClaim(id: String) {
        updateState { copy(destination = OwnershipDestination.Claim, selectedClaim = null, selectedClaimId = id, loading = true, problem = null) }
        guarded { repository.claim(id) }.onSuccess { claim -> updateState { copy(selectedClaim = claim, loading = false) } }.onFailure(::failed)
    }
    private suspend fun openProfile(kind: BusinessKind, id: String) {
        updateState { copy(destination = OwnershipDestination.Editor, editingBusiness = OwnedBusiness(id, kind, ""), profile = null, draft = null, loading = true, problem = null, saved = false) }
        guarded { repository.profile(kind, id) }.onSuccess(::applyProfile).onFailure(::failed)
    }
    private fun applyProfile(profile: BusinessProfile) = updateState {
        copy(profile = profile, draft = ProfileDraft.from(profile), loading = false, busy = false,
            reloadRequired = false, problem = null, confirmDiscard = false, confirmPublish = false)
    }
    private suspend fun reload() {
        val business = currentState.editingBusiness ?: return
        openProfile(business.kind, business.id)
    }
    private suspend fun submit() {
        val value = try { currentState.submission.validated() } catch (failure: Exception) { return failed(failure) }
        updateState { copy(busy = true, problem = null) }
        guarded { repository.submit(value) }.onSuccess { claim -> updateState {
            copy(busy = false, destination = OwnershipDestination.Claim, selectedClaim = claim, selectedClaimId = claim.id,
                submission = ClaimSubmission(BusinessKind.CoffeeShop, "", "", ""))
        } }.onFailure(::failed)
    }
    private fun askSave() {
        if (currentState.reloadRequired || !currentState.dirty) return
        try { currentState.draft?.validate() } catch (failure: Exception) { return failed(failure) }
        updateState { copy(confirmPublish = true, problem = null) }
    }
    private suspend fun save() {
        val snapshot = currentState
        if (!snapshot.confirmPublish || snapshot.reloadRequired) return
        val profile = snapshot.profile ?: return
        val draft = snapshot.draft ?: return
        updateState { copy(busy = true, confirmPublish = false, problem = null) }
        guarded { repository.save(profile, draft) }.onSuccess { updated ->
            applyProfile(updated)
            updateState { copy(saved = true) }
        }.onFailure(::failed)
    }
    private suspend fun back() {
        if (currentState.busy) return
        operations.toList().forEach { it.cancel() }
        updateState { copy(loading = false) }
        if (currentState.destination == OwnershipDestination.Overview) sendEvent(OwnershipEvent.Close)
        else if (currentState.dirty || currentState.destination == OwnershipDestination.Submit && currentState.submission.hasContent()) {
            updateState { copy(confirmDiscard = true, reloadAfterDiscard = false) }
        } else if (initialKind != null) sendEvent(OwnershipEvent.Close) else overview()
    }
    private suspend fun <T> guarded(block: suspend () -> Result<T>): Result<T> {
        val snapshot = currentState
        val result = block()
        currentCoroutineContext().ensureActive()
        if (snapshot.generation != currentState.generation || snapshot.account != account.current()) throw CancellationException("Ownership account changed")
        return result
    }
    private fun failed(error: Throwable) = updateState {
        val issue = (error as? OwnershipFailure)?.problem ?: OwnershipProblem.Unavailable
        when (issue) {
            OwnershipProblem.SignedOut -> OwnershipState(account = this@OwnershipViewModel.account.current(), generation = generation, problem = issue)
            OwnershipProblem.NotFound -> copy(loading = false, busy = false, problem = issue, profile = null, draft = null)
            else -> copy(loading = false, busy = false, problem = issue,
                reloadRequired = reloadRequired || issue == OwnershipProblem.Conflict || issue == OwnershipProblem.PartialSave)
        }
    }
}
private fun ClaimSubmission.edit(field: ClaimField, value: String) = when (field) {
    ClaimField.Slug -> copy(slug = value)
    ClaimField.Contact -> copy(contact = value)
    ClaimField.Evidence -> copy(evidence = value)
}
private fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private fun ClaimSubmission.hasContent(): Boolean = contact.isNotBlank() || evidence.isNotBlank() || photos.isNotEmpty()
