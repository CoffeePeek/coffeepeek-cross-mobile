package com.coffeepeek.feature.profile.impl.ui.model

import com.coffeepeek.feature.profile.api.OwnProfile

internal enum class OwnProfileError { LoadFailed }

internal data class OwnProfileState(
    val isLoggedIn: Boolean = false,
    val profile: OwnProfile? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: OwnProfileError? = null,
    val refreshError: OwnProfileError? = null,
) {
    val publicSlug: String? get() = profile?.address?.slug?.takeIf(String::isNotBlank)
    val hasContent: Boolean get() = profile?.let { it.userName.isNotBlank() || !it.avatarUrl.isNullOrBlank() } == true
}

internal sealed interface OwnProfileAction {
    data object Initialize : OwnProfileAction
    data object Refresh : OwnProfileAction
    data object Logout : OwnProfileAction
}

internal sealed interface OwnProfileEvent
