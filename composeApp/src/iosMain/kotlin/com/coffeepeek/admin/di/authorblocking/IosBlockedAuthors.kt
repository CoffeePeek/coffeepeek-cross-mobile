package com.coffeepeek.admin.di.authorblocking

import com.coffeepeek.feature.authorblocking.api.AuthorBlockPrompt
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingListState
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Swift-facing snapshots of the existing application-owned moderation presenter. */
class IosBlockedAuthors internal constructor(private val presenter: AuthorBlockingPresenter) {
    private var promptVersion = 0L
    private var observedPrompt: IosAuthorBlockPrompt? = null

    fun currentState(): IosBlockedAuthorsState = snapshot(
        presenter.blocks.value, presenter.prompt.value, presenter.needsLogin.value,
    )

    fun refresh() = presenter.refresh()

    fun requestUnblock(authorId: String) {
        val state = presenter.blocks.value
        if (!state.signedIn || !state.ready || presenter.prompt.value?.working == true) return
        val author = state.items.firstOrNull { it.id == authorId } ?: return
        presenter.requestUnblock(AuthorReference(author.id, author.slug, author.name, author.avatarUrl))
    }

    fun confirm(expectedPrompt: IosAuthorBlockPrompt) {
        if (presenter.prompt.value === expectedPrompt.source) presenter.confirm()
    }

    fun dismiss(expectedPrompt: IosAuthorBlockPrompt) {
        if (presenter.prompt.value === expectedPrompt.source) presenter.dismiss()
    }
    fun loginHandled() = presenter.loginHandled()

    /** Cancelling a screen subscription leaves the shared presenter's lifetime with application DI. */
    fun observe(onState: (IosBlockedAuthorsState) -> Unit): IosBlockedAuthorsObservation {
        val job = CoroutineScope(Dispatchers.Main).launch {
            combine(presenter.blocks, presenter.prompt, presenter.needsLogin, ::snapshot)
                .collect { onState(it) }
        }
        return IosBlockedAuthorsObservation(job)
    }

    private fun snapshot(state: AuthorBlockingListState, prompt: AuthorBlockPrompt?, needsLogin: Boolean): IosBlockedAuthorsState {
        if (observedPrompt?.source !== prompt) {
            observedPrompt = prompt?.let { IosAuthorBlockPrompt(it, (++promptVersion).toString()) }
        }
        return stateSnapshot(state, observedPrompt, needsLogin)
    }
}

class IosBlockedAuthorsObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

data class IosBlockedAuthor(val id: String, val name: String, val slug: String?, val avatarUrl: String?)

class IosAuthorBlockPrompt internal constructor(internal val source: AuthorBlockPrompt, val id: String) {
    val name: String get() = source.author.name
    val blocking: Boolean get() = source.blocked
    val working: Boolean get() = source.working
    val failed: Boolean get() = source.error != null
}

data class IosBlockedAuthorsState(
    val items: List<IosBlockedAuthor>,
    val signedIn: Boolean,
    val ready: Boolean,
    val loading: Boolean,
    val error: String?,
    val prompt: IosAuthorBlockPrompt?,
    val needsLogin: Boolean,
)

private fun stateSnapshot(state: AuthorBlockingListState, prompt: IosAuthorBlockPrompt?, needsLogin: Boolean) =
    IosBlockedAuthorsState(
        items = state.items.map { IosBlockedAuthor(it.id, it.name, it.slug, it.avatarUrl) },
        signedIn = state.signedIn,
        ready = state.ready,
        loading = state.loading,
        error = state.error,
        prompt = prompt,
        needsLogin = needsLogin,
    )
