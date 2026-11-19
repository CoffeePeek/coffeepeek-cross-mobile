package com.coffeepeek.feature.profile.impl.ui.model

import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind

internal sealed interface PublicProfileAction {
    data object Initialize : PublicProfileAction
    data object Refresh : PublicProfileAction
    data object Resume : PublicProfileAction
    data class ToggleCheckInLike(val id: String) : PublicProfileAction
    data object ToggleFollow : PublicProfileAction
    data object RetryFavorites : PublicProfileAction
    data class ChangeActivityMonth(val month: String) : PublicProfileAction
    data object RetryActivity : PublicProfileAction
    data class OpenConnections(val kind: PublicProfileConnectionKind) : PublicProfileAction
    data object CloseConnections : PublicProfileAction
    data object RetryConnections : PublicProfileAction
    data object LoadMoreConnections : PublicProfileAction
    data object LoadMoreCheckIns : PublicProfileAction
    data object RetryCheckIns : PublicProfileAction
    data class SelectCheckInDate(val date: String?) : PublicProfileAction
}

internal sealed interface PublicProfileEvent {
    /** A guest or expired session needs sign-in before an account action. */
    data object RequireLogin : PublicProfileEvent
}
