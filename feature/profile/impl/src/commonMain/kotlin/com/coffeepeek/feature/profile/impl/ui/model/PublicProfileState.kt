package com.coffeepeek.feature.profile.impl.ui.model

import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

internal enum class PublicProfileError {
    BlocksLoad, Unavailable, Load, Verification, Follow, Favorites, Activity, Connections, CheckIns,
}

internal data class PublicProfileState(
    val isLoading: Boolean = true,
    val error: PublicProfileError? = null,
    val profile: PublicProfile? = null,
    val checkIns: List<PublicProfileCheckIn> = emptyList(),
    val isLoadingCheckIns: Boolean = false,
    val isLoadingMoreCheckIns: Boolean = false,
    val checkInsError: PublicProfileError? = null,
    val hasMoreCheckIns: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowLoading: Boolean = false,
    val followError: PublicProfileError? = null,
    val isLoadingFavorites: Boolean = false,
    val favoritesError: PublicProfileError? = null,
    val isLoadingActivity: Boolean = false,
    val activityError: PublicProfileError? = null,
    val activityMonth: String = currentActivityMonth(),
    val connectionKind: PublicProfileConnectionKind? = null,
    val connections: List<PublicProfileConnection> = emptyList(),
    val isLoadingConnections: Boolean = false,
    val isLoadingMoreConnections: Boolean = false,
    val connectionsError: PublicProfileError? = null,
    val hasMoreConnections: Boolean = false,
)

/** The month shown first is the current one in Minsk (UTC+3), as the server counts activity. */
@OptIn(ExperimentalTime::class)
internal fun currentActivityMonth(): String = (Clock.System.now() + 3.hours).toString().take(7)
