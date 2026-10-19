package com.coffeepeek.feature.roaster.impl.ui.model

import com.coffeepeek.feature.roaster.domain.model.RoasterDetails

internal data class RoasterDetailState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val details: RoasterDetails? = null,
    val favoriteKeys: Set<String> = emptySet(),
    val previewPhotoUrl: String? = null,
    val message: RoasterMessage? = null,
) {
    val isFavorite: Boolean get() = details?.let { it.address.slug.ifBlank { it.id } in favoriteKeys } == true
}

internal sealed interface RoasterDetailAction {
    data object Initialize : RoasterDetailAction
    data object Retry : RoasterDetailAction
    data object Back : RoasterDetailAction
    data object ToggleFavorite : RoasterDetailAction
    data class OpenPhoto(val url: String) : RoasterDetailAction
    data object ClosePhoto : RoasterDetailAction
    data class OpenShop(val slug: String) : RoasterDetailAction
    data object OpenInstagram : RoasterDetailAction
    data object OpenWebsite : RoasterDetailAction
    data object DismissMessage : RoasterDetailAction
}

internal sealed interface RoasterDetailEvent {
    data object Back : RoasterDetailEvent
    data class OpenShop(val slug: String) : RoasterDetailEvent
    /** Already normalized to an absolute URL. */
    data class OpenLink(val url: String) : RoasterDetailEvent
}
