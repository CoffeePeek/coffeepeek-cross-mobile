package com.coffeepeek.feature.community.impl.post

import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.api.EditField
import com.coffeepeek.feature.community.impl.timeline.PostEditState
import com.coffeepeek.feature.community.impl.timeline.EditError
import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.domain.rules.MAX_REPORT_LENGTH
import com.coffeepeek.feature.community.domain.rules.validateReport
import com.coffeepeek.feature.community.impl.timeline.ActionMessage
import com.coffeepeek.feature.community.impl.timeline.MessageKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * One post opened from a shared link, with the feed's rules: one account at a time (a session change reloads it and drops late answers),
 * one mutation at a time, likes and reports only on a published post of someone else, and a blocked author's post is hidden.
 */
internal class PostViewModel(
    private val id: String,
    private val repository: CommunityRepository,
    private val account: CommunityAccount,
    private val authors: AuthorVisibility = AuthorVisibility.None,
    private val analytics: CommunityAnalytics,
    private val drinks: ConsumedDrinks,
) : MviViewModel<PostState, PostAction, PostEvent>(PostState()) {
    private var accountKey: String? = null
    private var generation = 0L
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private var profileJob: Job? = null
    private var drinksJob: Job? = null

    init {
        viewModelScope.launch {
            // Blocking the author from this screen hides the post at once, as the feed drops their posts.
            authors.visibility.collect { if (currentState.post?.let(::isBlocked) == true) hideBlocked() }
        }
        viewModelScope.launch { account.accountKey.distinctUntilChanged().collect { switchAccount(it) } }
    }

    private fun switchAccount(key: String?) {
        generation++
        loadJob?.cancel(); mutationJob?.cancel(); profileJob?.cancel()
        drinksJob?.cancel()
        accountKey = key
        updateState { PostState(isLoggedIn = key != null) }
        load()
        if (key != null) profileJob = viewModelScope.launch {
            val result = account.ownAuthorSlug()
            currentCoroutineContext().ensureActive()
            if (isCurrent(key)) result.onSuccess { slug -> updateState { copy(ownAuthorSlug = slug?.takeIf(String::isNotBlank)) } }
        }
    }

    private fun isCurrent(key: String?) =
        currentState.isLoggedIn != null && accountKey == key && account.currentAccountKey() == key

    private fun isBlocked(post: CommunityPost): Boolean =
        authors.visibility.value.let { it.ready && it.hides(post.author.id, post.author.slug) }

    private fun hideBlocked() {
        generation++
        mutationJob?.cancel()
        updateState { PostState(isLoggedIn = isLoggedIn, ownAuthorSlug = ownAuthorSlug, error = PostError.Unavailable) }
    }

    override suspend fun handleActionInternal(action: PostAction) {
        when (action) {
            PostAction.Edit -> openEdit()
            is PostAction.EditText -> updateEdit { copy(text = action.text) }
            is PostAction.EditRating -> updateEdit { copy(rating = action.rating) }
            is PostAction.EditDrink -> updateEdit { copy(drinkSlug = action.slug, customDrinkName = action.customName) }
            PostAction.LoadDrinks -> if (currentState.editing != null) loadDrinks()
            PostAction.SaveEdit -> saveEdit()
            PostAction.DismissEdit -> dismissEdit()
            else -> handlePostAction(action)
        }
    }

    private suspend fun handlePostAction(action: PostAction) {
        when (action) {
            PostAction.Reload -> if (!currentState.isMutating && currentState.editing == null) load()
            PostAction.Retry -> if (currentState.error == PostError.Failed) load()
            PostAction.ToggleHelpful -> toggleHelpful()
            PostAction.OpenReport -> openReport()
            is PostAction.ReportText -> if (!currentState.isReporting) {
                updateState { copy(reportText = action.text.take(MAX_REPORT_LENGTH), reportIssue = null, reportFailed = false, reportFailure = null) }
            }
            PostAction.SubmitReport -> submitReport()
            PostAction.DismissReport -> if (!currentState.isReporting) {
                updateState { copy(isReportOpen = false, reportText = "", reportIssue = null, reportFailed = false, reportFailure = null) }
            }
            else -> Unit
        }
    }

    private fun load() {
        val key = accountKey
        if (!isCurrent(key)) return
        loadJob?.cancel()
        updateState { copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            val result = repository.postById(id)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { post ->
                if (isBlocked(post)) hideBlocked() else updateState { copy(isLoading = false, post = post) }
            }.onFailure { error ->
                val unavailable = (error as? CommunityFailure)?.kind == FailureKind.NotFound
                updateState { copy(isLoading = false, error = if (unavailable) PostError.Unavailable else PostError.Failed) }
            }
        }
    }

    private suspend fun toggleHelpful() {
        val current = currentState
        val post = current.post ?: return
        if (!current.isPublished || current.isOwn || current.isMutating || current.editing != null) return
        val key = accountKey ?: run { sendEvent(PostEvent.SignInRequested); return }
        if (!isCurrent(key)) return
        loadJob?.cancel()
        updateState { copy(isVoting = true, post = post.copy(isHelpfulByCurrentUser = !post.isHelpfulByCurrentUser,
            helpfulCount = (post.helpfulCount + if (post.isHelpfulByCurrentUser) -1 else 1).coerceAtLeast(0))) }
        val epoch = generation
        mutationJob = viewModelScope.launch { submitHelpful(post, key, epoch) }
    }

    private suspend fun submitHelpful(post: CommunityPost, key: String, epoch: Long) {
        try {
            val result = repository.setHelpful(post.id, !post.isHelpfulByCurrentUser)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            currentCoroutineContext().ensureActive()
            if (!isCurrentVote(key, epoch, post.id)) return
            finishHelpful(post, result.getOrNull())
            result.exceptionOrNull()?.let { reportVoteFailure(it) }
        } catch (cancelled: CancellationException) {
            if (isCurrentVote(key, epoch, post.id)) finishHelpful(post, null)
            throw cancelled
        } catch (error: Exception) {
            if (isCurrentVote(key, epoch, post.id)) {
                finishHelpful(post, null)
                reportVoteFailure(error)
            }
        }
    }

    private fun isCurrentVote(key: String, epoch: Long, id: String) =
        isCurrent(key) && epoch == generation && currentState.post?.id == id

    private fun finishHelpful(original: CommunityPost, vote: HelpfulVote?) = updateState {
        copy(isVoting = false, post = post?.copy(helpfulCount = vote?.helpfulCount ?: original.helpfulCount,
            isHelpfulByCurrentUser = vote?.isHelpful ?: original.isHelpfulByCurrentUser))
    }

    private suspend fun reportVoteFailure(error: Throwable) = sendEvent(
        PostEvent.Message(ActionMessage(MessageKind.VoteFailed, (error as? CommunityFailure)?.serverMessage)),
    )

    private suspend fun openReport() {
        val current = currentState
        if (!current.isPublished || current.isOwn || current.isMutating || current.editing != null) return
        if (accountKey == null) { sendEvent(PostEvent.SignInRequested); return }
        updateState { copy(isReportOpen = true, reportText = "", reportIssue = null, reportFailed = false, reportFailure = null) }
    }

    private fun submitReport() {
        val current = currentState
        val post = current.post ?: return
        val key = accountKey ?: return
        if (!current.isReportOpen || !isCurrent(key) || current.isMutating) return
        val text = current.reportText.trim()
        validateReport(text)?.let { issue -> updateState { copy(reportIssue = issue) }; return }
        updateState { copy(isReporting = true) }
        mutationJob = viewModelScope.launch {
            val result = repository.report(post.id, text)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess {
                updateState { copy(isReporting = false, isReportOpen = false, reportText = "") }
                sendEvent(PostEvent.Message(ActionMessage(MessageKind.ReportSent)))
            }.onFailure { error ->
                updateState { copy(isReporting = false, reportFailed = true, reportFailure = (error as? CommunityFailure)?.serverMessage) }
            }
        }
    }
    private fun openEdit() {
        val current = currentState
        val post = current.post ?: return
        if (!current.isOwn || current.isMutating || current.isReportOpen || post.isHome || !isCurrent(accountKey)) return
        analytics.editOpened()
        updateState { copy(editing = PostEditState(post)) }
        loadDrinks()
    }

    private fun updateEdit(change: PostEditState.() -> PostEditState) {
        val before = currentState.editing ?: return
        if (currentState.isSaving) return
        val after = before.change()
        if (before.text != after.text) analytics.editFieldChanged(EditField.Note)
        if (before.rating != after.rating) analytics.editFieldChanged(EditField.Rating)
        if (before.drinkSlug != after.drinkSlug || before.customDrinkName != after.customDrinkName) analytics.editFieldChanged(EditField.Drink)
        updateState { copy(editing = after.copy(error = null)) }
    }

    private fun dismissEdit() {
        if (currentState.isSaving) return
        analytics.editCancelled()
        updateState { copy(editing = null) }
    }

    private fun loadDrinks() {
        val key = accountKey ?: return
        drinksJob?.cancel()
        drinksJob = viewModelScope.launch {
            val result = drinks.load()
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { options -> updateState { copy(drinks = options, drinksFailed = false) } }
                .onFailure { updateState { copy(drinksFailed = true) } }
        }
    }

    private fun saveEdit() {
        val edit = currentState.editing ?: return
        val key = accountKey ?: return
        if (!isCurrent(key) || currentState.isMutating) return
        analytics.editSubmitted()
        edit.issue()?.let { issue ->
            analytics.editRejected()
            updateState { copy(editing = edit.copy(error = EditError.Invalid(issue))) }
            return
        }
        submitEdit(edit, key)
    }

    private fun submitEdit(edit: PostEditState, key: String) {
        loadJob?.cancel()
        updateState { copy(isSaving = true, isLoading = false) }
        mutationJob = viewModelScope.launch {
            val result = repository.update(edit.source.id, edit.input())
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { updated ->
                analytics.editSaved(updated.visibility, updated.photos.originals.isNotEmpty())
                updateState { copy(post = updated, editing = null, isSaving = false) }
                sendEvent(PostEvent.Message(ActionMessage(MessageKind.EditSaved)))
            }.onFailure { error ->
                analytics.editFailed(error)
                updateState { copy(isSaving = false, editing = edit.copy(error = EditError.Server((error as? CommunityFailure)?.serverMessage))) }
            }
        }
    }

}
