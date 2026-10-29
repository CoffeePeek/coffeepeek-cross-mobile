package com.coffeepeek.feature.profile.impl.ui.model

import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

internal data class PublicProfileState(
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = false,
    val error: String? = null,
    val profile: PublicProfile? = null,
    val checkIns: List<PublicProfileCheckIn> = emptyList(),
    val isLoadingCheckIns: Boolean = false,
    val isLoadingMoreCheckIns: Boolean = false,
    val checkInsError: String? = null,
    val hasMoreCheckIns: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowLoading: Boolean = false,
    val followError: String? = null,
    val isLoadingFavorites: Boolean = false,
    val favoritesError: String? = null,
    val isLoadingActivity: Boolean = false,
    val activityError: String? = null,
    val activityMonth: String = currentActivityMonth(),
    val connectionKind: PublicProfileConnectionKind? = null,
    val connections: List<PublicProfileConnection> = emptyList(),
    val isLoadingConnections: Boolean = false,
    val isLoadingMoreConnections: Boolean = false,
    val connectionsError: String? = null,
    val hasMoreConnections: Boolean = false,
)

/** The month shown first is the current one in Minsk (UTC+3), as the server counts activity. */
@OptIn(ExperimentalTime::class)
internal fun currentActivityMonth(): String = (Clock.System.now() + 3.hours).toString().take(7)
