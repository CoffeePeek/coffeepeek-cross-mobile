package com.coffeepeek.feature.roaster.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.roaster.api.RoasterDetailAnalytics
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.impl.ui.RoasterDetailViewModel
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import com.coffeepeek.feature.roaster.impl.ui.data.instagramDisplayText
import com.coffeepeek.feature.roaster.impl.ui.data.prettyExternalLink
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter

typealias NativeRoasterDetailContent = @Composable (NativeRoasterDetailSession, NativeRoasterDetailHost) -> Boolean

class NativeRoasterDetailHost(
    val onBack: () -> Unit, val onOpenShop: (String) -> Unit, val onOpenLink: (String) -> Unit,
    val onShare: (String) -> Unit, val onFavoriteFeedback: () -> Unit, val modifier: Modifier,
)

fun createNativeRoasterDetailSession(slug: String, repository: RoasterRepository,
    favorites: RoasterFavoritesPort = RoasterFavoritesPort.None,
    shopPhotos: RoasterShopPhotos = RoasterShopPhotos.None,
    analytics: RoasterDetailAnalytics = RoasterDetailAnalytics.None): NativeRoasterDetailSession {
    val model = RoasterDetailViewModel(slug, repository, favorites, shopPhotos, analytics)
    val store = ViewModelStore().apply { put("native-roaster-detail", model) }
    return NativeRoasterDetailSession(model, store::clear)
}

class NativeRoasterDetailSession internal constructor(private val model: RoasterDetailViewModel,
    private val dispose: () -> Unit = {}) {
    private var closed = false
    val state = model.state.map { current() }
    val events = model.events.filter { !closed }.map { when (it) {
        RoasterDetailEvent.Back -> NativeRoasterEvent("back", "")
        is RoasterDetailEvent.OpenShop -> NativeRoasterEvent("shop", it.slug)
        is RoasterDetailEvent.OpenLink -> NativeRoasterEvent("link", it.url)
    } }
    fun current() = if (closed) NativeRoasterDetailState(false, false, null, false, null, null, null, null, null)
        else model.state.value.native()
    fun retry() = dispatch(RoasterDetailAction.Retry)
    fun back() = dispatch(RoasterDetailAction.Back)
    fun favorite() = dispatch(RoasterDetailAction.ToggleFavorite)
    fun photo(url: String) = dispatch(RoasterDetailAction.OpenPhoto(url))
    fun closePhoto() = dispatch(RoasterDetailAction.ClosePhoto)
    fun shop(slug: String) = dispatch(RoasterDetailAction.OpenShop(slug))
    fun instagram() = dispatch(RoasterDetailAction.OpenInstagram)
    fun website() = dispatch(RoasterDetailAction.OpenWebsite)
    fun dismissMessage() = dispatch(RoasterDetailAction.DismissMessage)
    fun close() { if (!closed) { closed = true; dispose() } }
    private fun dispatch(action: RoasterDetailAction) { if (!closed) model.onAction(action) }
}

data class NativeRoasterEvent(val kind: String, val value: String)
data class NativeRoasterDetailState(
    val loading: Boolean, val failed: Boolean, val details: RoasterDetails?,
    val favorite: Boolean, val photo: String?, val messageKey: String?, val message: String?,
    val instagram: String?, val website: String?,
)

private fun RoasterDetailState.native() = NativeRoasterDetailState(
    isLoading, loadFailed, details, isFavorite, previewPhotoUrl, message.messageKey(), (message as? RoasterMessage.FromStore)?.text,
    details?.contact?.instagramLink?.takeIf(String::isNotBlank)?.let(::instagramDisplayText),
    details?.contact?.siteLink?.takeIf(String::isNotBlank)?.let(::prettyExternalLink),
)

internal fun RoasterMessage?.messageKey() = when (this) {
    RoasterMessage.FavoritesUnavailable -> "roaster_message_favorites_unavailable"
    RoasterMessage.FavoriteChangeFailed -> "roaster_message_favorite_failed"
    else -> null
}
