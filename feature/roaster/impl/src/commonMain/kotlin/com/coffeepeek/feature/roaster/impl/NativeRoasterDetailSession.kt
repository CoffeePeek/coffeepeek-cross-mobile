package com.coffeepeek.feature.roaster.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.impl.ui.RoasterDetailViewModel
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import com.coffeepeek.feature.roaster.impl.ui.data.instagramDisplayText
import com.coffeepeek.feature.roaster.impl.ui.data.prettyExternalLink
import kotlinx.coroutines.flow.map

typealias NativeRoasterDetailContent = @Composable (NativeRoasterDetailSession, NativeRoasterDetailHost) -> Boolean

class NativeRoasterDetailHost(
    val onBack: () -> Unit, val onOpenShop: (String) -> Unit, val onOpenLink: (String) -> Unit,
    val onShare: (String) -> Unit, val onFavoriteFeedback: () -> Unit, val modifier: Modifier,
)

class NativeRoasterDetailSession internal constructor(private val model: RoasterDetailViewModel) {
    val state = model.state.map { it.native() }
    val events = model.events.map { when (it) {
        RoasterDetailEvent.Back -> NativeRoasterEvent("back", "")
        is RoasterDetailEvent.OpenShop -> NativeRoasterEvent("shop", it.slug)
        is RoasterDetailEvent.OpenLink -> NativeRoasterEvent("link", it.url)
    } }
    fun current() = model.state.value.native()
    fun retry() = model.onAction(RoasterDetailAction.Retry)
    fun back() = model.onAction(RoasterDetailAction.Back)
    fun favorite() = model.onAction(RoasterDetailAction.ToggleFavorite)
    fun photo(url: String) = model.onAction(RoasterDetailAction.OpenPhoto(url))
    fun closePhoto() = model.onAction(RoasterDetailAction.ClosePhoto)
    fun shop(slug: String) = model.onAction(RoasterDetailAction.OpenShop(slug))
    fun instagram() = model.onAction(RoasterDetailAction.OpenInstagram)
    fun website() = model.onAction(RoasterDetailAction.OpenWebsite)
    fun dismissMessage() = model.onAction(RoasterDetailAction.DismissMessage)
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
