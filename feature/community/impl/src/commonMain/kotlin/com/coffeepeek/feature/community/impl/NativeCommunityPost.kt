package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.post.*
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private class PostInput(val state: PostState, val resources: NativeCommunityResources, val photo: NativeCommunityPhoto?)

/** The ordinary detail binding preserves the same PostViewModel rules as the Android entry. */
class NativeCommunityPost(id: String, repository: CommunityRepository, account: CommunityAccount, authors: AuthorVisibility,
    analytics: CommunityAnalytics, drinks: ConsumedDrinks, private val blocks: AuthorBlockActions,
    private val host: CommunityNativeHost, private val navigation: CommunityPostNavigation,
) {
    private val owner = NativeCommunityPostSession(id, repository, account, authors, analytics, drinks)
    private val lifetime = NativeCommunityLifetime(account, host)
    private val photo = MutableStateFlow<NativeCommunityPhoto?>(null)
    private val mutableState = MutableStateFlow<NativeCommunityPostSnapshot?>(null)
    val state = mutableState.asStateFlow()
    private val mutableActive = MutableStateFlow(true)
    val active = mutableActive.asStateFlow()
    private var photoKey = 0L

    init {
        lifetime.start(::close)
        observePresentation()
        observeEvents()
    }

    fun presentation(started: Boolean) = lifetime.presentation(started, 0.0)
    fun resumed() {
        if (lifetime.isCurrent()) owner.resumed()
    }

    private fun observePresentation() {
        lifetime.scope.launch {
            combine(owner.model.state, lifetime.resources.filterNotNull(), photo, lifetime.clock) { state, resources, photo, _ ->
                PostInput(state, resources, photo)
            }.collectLatest { input ->
                if (input.state.post == null) photo.value = null
                val snapshot = snapshot(input)
                currentCoroutineContext().ensureActive()
                if (lifetime.isCurrent()) mutableState.value = snapshot
            }
        }
    }

    private suspend fun snapshot(input: PostInput): NativeCommunityPostSnapshot {
        val state = input.state
        val resources = input.resources
        val callbacks = state.post?.let {
            postActions(it, state, owner::perform, navigation, blocks, resources.text(Res.string.community_user),
                resources.sharer(host.share), ::openPhoto)
        }
        val card = callbacks?.let { card(input, it) }
        val actions = callbacks?.let { nativePostActions(it, !state.isMutating, false) }
        val guarded = guardedActions(state, actions)
        return NativeCommunityPostSnapshot(nativePostDetailState(state, card, resources.detailTexts()), guarded,
            sheet(state, resources), input.photo.takeIf { state.post != null && state.error == null })
    }

    private fun guardedActions(state: PostState, actions: CommunityCardActions?): CommunityCardActions {
        val commands = actions?.actions.orEmpty().mapValues { (_, action) -> { if (currentPost(state)) action() } } +
            mapOf("back" to { if (interactive()) navigation.onBack() }, "retry" to { if (interactive()) owner.perform(PostAction.Retry) })
        return CommunityCardActions(commands) { if (currentPost(state)) actions?.onPhoto?.invoke(it) }
    }

    private fun currentPost(state: PostState) = state.post != null && owner.model.state.value.post == state.post && interactive()

    private suspend fun card(input: PostInput, actions: PostCardActions): CommunityCardPresentation {
        val state = input.state
        val post = requireNotNull(state.post)
        val texts = input.resources.postTexts()
        return nativePostPresentation(post, state.isOwn, !state.isOwn, state.isPublished || post.helpfulCount > 0,
            !state.isMutating, false, actions, texts, input.resources.postFormatted(post, state.isOwn, actions.onHelpful != null, texts),
            post.createdAt.takeIf(String::isNotBlank)?.let(host.displayDate).orEmpty(), input.resources.language, host.brewMethodLabel, 16.0)
    }

    private suspend fun sheet(state: PostState, resources: NativeCommunityResources): NativeCommunitySheet? {
        state.editing?.let { edit ->
            val data = nativeEditorPresentation(edit, state.drinks, state.drinksFailed, state.isSaving, resources.language,
                resources.editorTexts(), resources.editorRatings(edit), resources.editError(edit.error))
            val send: (PostAction) -> Unit = { if (lifetime.isCurrent() && owner.model.state.value.editing?.source?.id == edit.source.id) owner.perform(it) }
            return NativeCommunitySheet.Editor("edit:${edit.source.id}", data,
                nativeEditorActions(edit, state.drinks, postEditorCallbacks(send)))
        }
        if (!state.isReportOpen) return null
        val send: (PostAction) -> Unit = { if (lifetime.isCurrent() && owner.model.state.value.isReportOpen) owner.perform(it) }
        return NativeCommunitySheet.Report("report", nativeReportPresentation(state.reportForm(), resources.reportTexts()),
            nativeReportActions(postReportCallbacks(send)))
    }

    private fun interactive(): Boolean {
        val state = owner.model.state.value
        return lifetime.isCurrent() && lifetime.runtime.value.started && photo.value == null && state.editing == null && !state.isReportOpen
    }

    private fun openPhoto(preview: PostPhotoPreview) {
        if (!interactive() || preview.index !in preview.originals.indices) return
        val key = ++photoKey
        photo.value = NativeCommunityPhoto(key, preview.thumbnails, preview.originals, preview.index) {
            if (lifetime.isCurrent() && photo.value?.key == key) photo.value = null
        }
    }

    private fun observeEvents() {
        lifetime.scope.launch { lifetime.runtime.map { it.started }.distinctUntilChanged().collectLatest { started ->
            if (started) owner.model.events.collect { event ->
                if (!lifetime.isCurrent()) return@collect
                when (event) {
                    PostEvent.SignInRequested -> navigation.onSignIn()
                    is PostEvent.Message -> lifetime.resources.filterNotNull().first().messageTexts().let {
                        if (lifetime.isCurrent()) host.message(it.text(event.message))
                    }
                }
            }
        } }
    }

    fun close() {
        mutableActive.value = false
        lifetime.close()
        owner.close()
        photo.value = null
        mutableState.value = null
    }
}
