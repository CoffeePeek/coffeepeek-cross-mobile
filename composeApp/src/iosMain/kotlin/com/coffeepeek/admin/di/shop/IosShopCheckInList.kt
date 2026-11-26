@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shop

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.media.IosPhotoViewer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.IosShopCheckInListSession
import com.coffeepeek.feature.shop.impl.ShopCheckInListCallbacks
import com.coffeepeek.feature.shop.impl.ShopCheckInListRow
import com.coffeepeek.feature.shop.impl.createIosShopCheckInListSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import platform.UIKit.UIImage

class IosShopCheckInList internal constructor(
    private val shopId: String,
    sessions: SessionReader,
    visibility: AuthorVisibility,
    private val accounts: ProfileAccountPort,
    private val details: ShopDetailsRepository,
    private val votes: ShopCheckInVoteRepository,
    private val blockActions: AuthorBlockActions,
    private val media: IosCatalogueMedia,
    private val languageTag: String,
    private val formatDate: (String) -> String,
) {
    private val host = ShopBrowsingHostViewModel(sessions, visibility)
    private val store = ViewModelStore().apply { put("ios-shop-check-in-host", host) }
    private var binding: BoundShopCheckIns? = null
    private val visible = MutableStateFlow(false)

    fun currentState(): IosShopCheckInListState {
        val source = host.session.value
        return IosShopCheckInListState(source.generation, bind(source)?.model?.current(), source.observationFailed)
    }

    fun observe(
        onState: (IosShopCheckInListState) -> Unit,
        onPhoto: (IosPhotoViewer) -> Unit,
        unknownAuthor: String,
    ) = IosShopCheckInObservation(host.viewModelScope.launch {
        coroutineScope {
            launch { visible.collectLatest { shown -> if (shown) while (true) {
                delay(60_000)
                onState(currentState())
            } } }
            host.session.collectLatest { source ->
                val bound = bind(source)
                onState(IosShopCheckInListState(source.generation, bound?.model?.current(), source.observationFailed))
                if (bound != null) observeBinding(bound, onState, onPhoto, unknownAuthor)
            }
        }
    })

    fun dateLabel(raw: String) = formatDate(raw)
    fun back() = Navigator.popBack()
    fun resume() { currentModel()?.resume() }
    fun setVisible(shown: Boolean) {
        if (visible.value == shown) return
        visible.value = shown
        if (shown) resume()
    }
    fun retry() {
        if (host.session.value.observationFailed) host.retry()
        else currentModel()?.retry()
    }

    fun signIn() { currentModel()?.signIn() }
    fun register() { currentModel()?.register() }
    fun vote(id: String) { currentModel()?.vote(id) }
    fun report(id: String) { currentModel()?.report(id) }
    fun author(id: String) { currentModel()?.author(id) }
    fun block(id: String) { currentModel()?.block(id) }
    fun photo(id: String, index: Int) { currentModel()?.photo(id, index) }
    fun close() = store.clear()

    fun loadAvatar(id: String, receive: (UIImage?) -> Unit) = load(id, receive) { it.avatarUrl }
    fun loadPhoto(id: String, index: Int, receive: (UIImage?) -> Unit) = load(id, receive) { it.thumbnail(index) }

    private fun load(id: String, receive: (UIImage?) -> Unit, url: (ShopCheckInListRow) -> String?) =
        IosShopCheckInObservation(host.viewModelScope.launch {
            val bound = bind(host.session.value) ?: return@launch
            val row = bound.model.current().rows.firstOrNull { it.id == id } ?: return@launch
            val path = url(row)?.takeIf(String::isNotBlank) ?: return@launch
            try {
                media.observeImage(path, bound.source.lease::isCurrent).collect {
                    receive(it.takeIf { bound.source.lease.isCurrent() })
                }
            } catch (cancelled: CancellationException) {
                receive(null)
                throw cancelled
            } catch (_: Exception) { receive(null) }
        })

    private suspend fun observeBinding(
        bound: BoundShopCheckIns,
        onState: (IosShopCheckInListState) -> Unit,
        onPhoto: (IosPhotoViewer) -> Unit,
        unknownAuthor: String,
    ): Nothing = coroutineScope {
        val state = bound.model.observeState {
            if (bound.source.lease.isCurrent()) onState(IosShopCheckInListState(bound.source.generation, it))
        }
        val events = launch { visible.collectLatest { shown -> if (shown) {
            val listener = bound.model.observeEvents(ShopCheckInNavigation(bound.source.lease, blockActions, unknownAuthor, media, onPhoto))
            try { awaitCancellation() } finally { listener.cancel() }
        } } }
        try { awaitCancellation() } finally {
            state.cancel()
            events.cancel()
        }
    }

    private fun currentModel() = bind(host.session.value)?.model

    private fun bind(source: ShopBrowsingHostSession): BoundShopCheckIns? {
        if (source.observationFailed || !source.lease.isCurrent()) return null
        binding?.takeIf { it.source === source }?.let { return it }
        val model = createIosShopCheckInListSession(shopId, source.lease.details(details), source.lease.votes(votes),
            { source.lease.viewer(accounts) }, source.generation, source.viewModelStore, languageTag)
        return BoundShopCheckIns(source, model).also { binding = it }
    }
}

private class BoundShopCheckIns(val source: ShopBrowsingHostSession, val model: IosShopCheckInListSession)

private class ShopCheckInNavigation(
    private val lease: ShopBrowsingSession,
    private val blockActions: AuthorBlockActions,
    private val unknownAuthor: String,
    private val media: IosCatalogueMedia,
    private val onPhoto: (IosPhotoViewer) -> Unit,
) : ShopCheckInListCallbacks {
    override fun back() { if (lease.isCurrent()) Navigator.popBack() }
    override fun signIn() = navigate(Navigator.Screen.Auth)
    override fun register() = navigate(Navigator.Screen.Register)
    override fun report(id: String) = navigate(Navigator.Screen.ReportCheckIn(id))
    override fun author(slug: String, nickname: String, avatar: String?) =
        navigate(Navigator.Screen.UserProfile(slug, nickname, avatar))
    override fun block(author: ShopCheckInAuthor) =
        lease.requestAuthorBlock(author.copy(nickname = author.nickname.ifBlank { unknownAuthor }), blockActions)

    override fun photo(urls: List<String>, originals: List<String>, index: Int) {
        if (lease.isCurrent()) onPhoto(media.viewer(CoffeePeekPhotoViewerImages(urls, originals), index, lease::isCurrent))
    }

    private fun navigate(screen: Navigator.Screen) {
        if (lease.isCurrent()) Navigator.navigate(screen)
    }
}

class IosShopCheckInObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
