package com.coffeepeek.feature.community.impl.post

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.domain.rules.MAX_REPORT_LENGTH
import com.coffeepeek.feature.community.domain.rules.validateReport
import com.coffeepeek.feature.community.impl.timeline.ActionMessage
import com.coffeepeek.feature.community.impl.timeline.MessageKind
import kotlinx.coroutines.Job
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
) : MviViewModel<PostState, PostAction, PostEvent>(PostState()) {
    private var accountKey: String? = null
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            // Blocking the author from this screen hides the post at once, as the feed drops their posts.
            authors.visibility.collect { if (currentState.post?.let(::isBlocked) == true) hideBlocked() }
        }
        viewModelScope.launch { account.accountKey.distinctUntilChanged().collect { switchAccount(it) } }
    }

    private fun switchAccount(key: String?) {
        loadJob?.cancel(); mutationJob?.cancel(); profileJob?.cancel()
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
        mutationJob?.cancel()
        updateState { PostState(isLoggedIn = isLoggedIn, ownAuthorSlug = ownAuthorSlug, error = PostError.Unavailable) }
    }

    override suspend fun handleActionInternal(action: PostAction) {
        when (action) {
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
        if (!current.isPublished || current.isOwn || current.isMutating) return
        val key = accountKey ?: run { sendEvent(PostEvent.SignInRequested); return }
        if (!isCurrent(key)) return
        updateState { copy(isVoting = true) }
        mutationJob = viewModelScope.launch {
            val result = repository.setHelpful(post.id, !post.isHelpfulByCurrentUser)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { vote ->
                updateState { copy(isVoting = false, post = this.post?.copy(helpfulCount = vote.helpfulCount, isHelpfulByCurrentUser = vote.isHelpful)) }
            }.onFailure { error ->
                updateState { copy(isVoting = false) }
                sendEvent(PostEvent.Message(ActionMessage(MessageKind.VoteFailed, (error as? CommunityFailure)?.serverMessage)))
            }
        }
    }

    private suspend fun openReport() {
        val current = currentState
        if (!current.isPublished || current.isOwn || current.isMutating) return
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
}
