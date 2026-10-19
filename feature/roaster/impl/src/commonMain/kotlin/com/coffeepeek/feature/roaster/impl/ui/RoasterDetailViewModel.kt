package com.coffeepeek.feature.roaster.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.roaster.api.RoasterContentAction
import com.coffeepeek.feature.roaster.api.RoasterDetailAnalytics
import com.coffeepeek.feature.roaster.api.RoasterDetailSection
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.ui.data.normalizeExternalUrl
import com.coffeepeek.feature.roaster.impl.ui.data.normalizeInstagramUrl
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class RoasterDetailViewModel(
    private val slug: String,
    private val repository: RoasterRepository,
    private val favorites: RoasterFavoritesPort = RoasterFavoritesPort.None,
    private val shopPhotos: RoasterShopPhotos = RoasterShopPhotos.None,
    private val analytics: RoasterDetailAnalytics = RoasterDetailAnalytics.None,
) : MviViewModel<RoasterDetailState, RoasterDetailAction, RoasterDetailEvent>(RoasterDetailState()) {
    private var initialized = false
    private var loadJob: Job? = null
    private var loadRevision = 0L

    init { onAction(RoasterDetailAction.Initialize) }

    override suspend fun handleActionInternal(action: RoasterDetailAction) {
        when (action) {
            RoasterDetailAction.Initialize -> if (!initialized) {
                initialized = true
                coroutineScope {
                    launch { observeFavoriteKeys() }
                    launch { observeFavoriteErrors() }
                    launch { load() }
                }
            }
            RoasterDetailAction.Retry -> load()
            RoasterDetailAction.Back -> sendEvent(RoasterDetailEvent.Back)
            RoasterDetailAction.ToggleFavorite -> toggleFavorite()
            is RoasterDetailAction.OpenPhoto -> if (currentState.details?.photos?.any { it.url == action.url } == true) {
                analytics.contentInteracted(RoasterDetailSection.Photos, RoasterContentAction.Open)
                updateState { copy(previewPhotoUrl = action.url) }
            }
            RoasterDetailAction.ClosePhoto -> updateState { copy(previewPhotoUrl = null) }
            is RoasterDetailAction.OpenShop -> if (currentState.details?.shops?.any { it.slug == action.slug } == true) {
                analytics.contentInteracted(RoasterDetailSection.Shops, RoasterContentAction.Select)
                analytics.shopOpened()
                sendEvent(RoasterDetailEvent.OpenShop(action.slug))
            }
            RoasterDetailAction.OpenInstagram -> currentState.details?.contact?.instagramLink?.let {
                analytics.contentInteracted(RoasterDetailSection.Contacts, RoasterContentAction.Instagram)
                sendEvent(RoasterDetailEvent.OpenLink(normalizeInstagramUrl(it)))
            }
            RoasterDetailAction.OpenWebsite -> currentState.details?.contact?.siteLink?.let {
                analytics.contentInteracted(RoasterDetailSection.Contacts, RoasterContentAction.Website)
                sendEvent(RoasterDetailEvent.OpenLink(normalizeExternalUrl(it)))
            }
            RoasterDetailAction.DismissMessage -> updateState { copy(message = null) }
        }
    }

    private suspend fun observeFavoriteKeys() {
        try {
            favorites.observeKeys().collect { keys -> updateState { copy(favoriteKeys = keys) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { updateState { copy(message = RoasterMessage.FavoritesUnavailable) } }
    }

    private suspend fun observeFavoriteErrors() {
        try {
            favorites.observeErrors().collect { error -> if (error != null) updateState { copy(message = RoasterMessage.FromStore(error)) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Error reporting is best effort. */ }
    }

    private fun toggleFavorite() {
        val state = currentState
        val details = state.details ?: return
        val key = details.address.slug.ifBlank { details.id }
        val target = RoasterFavoriteTarget(key, details.name, details.photos.firstOrNull()?.url,
            details.address.canonicalPath, details.address.revision, details.address.isAlias)
        favorites.change(target, favorite = !state.isFavorite) {
            updateState { copy(message = RoasterMessage.FavoriteChangeFailed) }
        }
    }

    private suspend fun load() {
        loadJob?.cancel()
        val job = currentCoroutineContext().job
        val revision = ++loadRevision
        loadJob = job
        updateState { copy(isLoading = true, loadFailed = false) }
        try {
            if (slug.isBlank() || slug.length > MAX_SLUG) {
                updateState { copy(loadFailed = true) }
                return
            }
            val details = repository.getRoaster(slug).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (loadJob !== job || revision != loadRevision) return
            updateState { copy(isLoading = false, details = details) }
            val shops = coroutineScope {
                details.shops.map { shop ->
                    async { if (!shop.photoUrl.isNullOrBlank()) shop else shop.copy(photoUrl = shopPhotos.firstPhoto(shop.slug)) }
                }.awaitAll()
            }
            currentCoroutineContext().ensureActive()
            if (loadJob !== job || revision != loadRevision) return
            updateState { if (this.details?.id == details.id) copy(details = this.details.copy(shops = shops)) else this }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (loadJob === job && revision == loadRevision && job.isActive) updateState { copy(loadFailed = true) }
        } finally {
            if (loadJob === job) { loadJob = null; updateState { copy(isLoading = false) } }
        }
    }

    override fun onCleared() {
        loadRevision++
        loadJob?.cancel()
        super.onCleared()
    }

    private companion object { const val MAX_SLUG = 200 }
}
