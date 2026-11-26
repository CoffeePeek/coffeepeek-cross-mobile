@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.coffeepeek.feature.profile.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileDate
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileWeek
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileWeekDates
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileError
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Clock

fun createIosPublicProfileSession(
    slug: String, data: PublicProfileData, checkIns: PublicCheckInsPort, accounts: ProfileAccountPort,
    visibility: ProfileAuthorVisibility, snapshots: ProfileSnapshotStore,
): IosPublicProfileSession {
    return IosPublicProfileSession(accounts, PublicProfileViewModel(slug, data, checkIns, accounts, visibility, snapshots))
}

class PublicProfileSnapshot internal constructor(
    state: PublicProfileState, visibility: ProfileAuthorVisibilityState, val active: Boolean,
    activities: Map<String, PublicProfileActivity>, today: String,
) {
    val visibilityRevision = visibility.revision
    val visibilityPending = !visibility.ready ||
        state.profile?.let { !it.isBlockedByViewer && visibility.hides(it.authorId, it.slug) } == true
    val profile = state.profile.takeIf { active && !visibilityPending }
    private val showsContent = profile?.isBlockedByViewer == false
    private val visibilityLoadFailed = !state.isLoading && state.error == PublicProfileError.BlocksLoad
    val loading = active && !visibilityLoadFailed && (state.isLoading || visibilityPending)
    val loadError = state.error?.name.takeIf { active && (!visibilityPending || visibilityLoadFailed) }
    val own = state.isOwnProfile && showsContent
    val signedIn = state.isSignedIn && showsContent
    val following = state.isFollowing && showsContent
    val followingCount = state.followingCount.takeIf { showsContent }
    val followEnabled = showsContent && !state.isFollowLoading && !state.isLoading
    val followFailed = showsContent && state.followError != null
    val checkIns = if (showsContent) state.checkIns.map(::OwnProfileCheckInSnapshot) else emptyList()
    val checkInsLoading = showsContent && (state.isLoadingCheckIns || state.isLoadingMoreCheckIns)
    val checkInsFailed = showsContent && state.checkInsError != null
    val hasMoreCheckIns = showsContent && state.hasMoreCheckIns
    val activityLoading = showsContent && activities.isEmpty() &&
        (state.isLoadingActivity || state.isLoadingCheckIns && checkIns.isEmpty())
    val activityFailed = showsContent && (state.activityError != null || state.checkInsError != null)
    val week = if (showsContent) publicProfileWeek(publicProfileWeekDates(today), activities, state.checkIns)
        .map { PublicProfileWeekCell(it.date, it.date == today, it.count, it.photos.firstOrNull()) } else emptyList()
    val connectionKind = state.connectionKind.takeIf { showsContent }
    val connections = state.connections.takeIf { connectionKind != null }.orEmpty()
    val connectionsLoading = connectionKind != null && (state.isLoadingConnections || state.isLoadingMoreConnections)
    val connectionsFailed = connectionKind != null && state.connectionsError != null
    val hasMoreConnections = connectionKind != null && state.hasMoreConnections

    val errorKey: String get() = when (loadError) {
        PublicProfileError.BlocksLoad.name -> "profile_blocks_load_failed"
        PublicProfileError.Unavailable.name -> "profile_unavailable"
        else -> "profile_load_failed"
    }
}

data class PublicProfileWeekCell(val date: String, val today: Boolean, val count: Int, val photo: String?)

class IosPublicProfileSession internal constructor(
    private val accounts: ProfileAccountPort,
    private val model: PublicProfileViewModel,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val store = ViewModelStore().apply { put("public-profile", model) }
    private val account = accounts.activeAccount()
    private val accountScope = accounts.cacheScope()
    private var closed = false
    private var activityRevision: Long? = null
    private var activitySlug: String? = null
    private val activities = mutableMapOf<String, PublicProfileActivity>()

    fun isCurrent() = !closed && account == accounts.activeAccount() && accountScope == accounts.cacheScope()

    fun current(): PublicProfileSnapshot {
        val state = model.state.value
        val visibility = model.currentVisibility()
        val today = requireNotNull(publicProfileDate(Clock.System.now().toString()))
        rememberActivity(state, visibility)
        return PublicProfileSnapshot(state, visibility, isCurrent(), activities, today)
    }

    fun observe(receive: (PublicProfileSnapshot) -> Unit): Job = scope.launch {
        combine(model.state, model.authorVisibility, accounts.observeActiveAccount(), accounts.observeCacheScope()) {
            _, _, _, _ -> current()
        }.collect { receive(it) }
    }

    fun observeLogin(open: () -> Unit): Job = scope.launch {
        model.events.collect { if (it == PublicProfileEvent.RequireLogin && isCurrent()) open() }
    }

    fun refresh() = send(PublicProfileAction.Refresh)
    fun toggleFollow() = contentAction(PublicProfileAction.ToggleFollow)
    fun retryActivity() = contentAction(PublicProfileAction.RetryActivity)
    fun retryCheckIns() = contentAction(PublicProfileAction.RetryCheckIns)
    fun loadMoreCheckIns() = contentAction(PublicProfileAction.LoadMoreCheckIns)
    fun openConnections(followers: Boolean) = contentAction(PublicProfileAction.OpenConnections(
        if (followers) PublicProfileConnectionKind.Followers else PublicProfileConnectionKind.Following,
    ))
    fun closeConnections() = send(PublicProfileAction.CloseConnections)
    fun retryConnections() = contentAction(PublicProfileAction.RetryConnections)
    fun loadMoreConnections() = contentAction(PublicProfileAction.LoadMoreConnections)

    fun close() {
        if (closed) return
        closed = true
        activities.clear()
        scope.cancel()
        store.clear()
    }

    private fun send(action: PublicProfileAction) {
        if (isCurrent()) model.onAction(action)
    }

    private fun contentAction(action: PublicProfileAction) {
        if (current().profile?.isBlockedByViewer == false) send(action)
    }

    private fun rememberActivity(state: PublicProfileState, visibility: ProfileAuthorVisibilityState) {
        val profile = state.profile
        if (visibility.revision != activityRevision || profile?.slug != activitySlug) activities.clear()
        activityRevision = visibility.revision
        activitySlug = profile?.slug
        if (!isCurrent() || !visibility.ready || profile == null || profile.isBlockedByViewer ||
            visibility.hides(profile.authorId, profile.slug)) return
        profile.activity?.let { activities[it.month] = it }
    }
}
