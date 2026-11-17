package com.coffeepeek.feature.profile.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileSignOutPort
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.impl.ui.OwnProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileState
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import com.coffeepeek.feature.profile.impl.ui.data.profileCheckInDrink
import com.coffeepeek.feature.profile.impl.ui.data.profileCheckInPhotos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun createIosOwnProfileSession(
    accounts: ProfileAccountPort, signOut: ProfileSignOutPort, data: PublicProfileData,
    checkIns: PublicCheckInsPort, visibility: ProfileAuthorVisibility, snapshots: ProfileSnapshotStore,
): IosOwnProfileSession = IosOwnProfileSession(accounts, OwnProfileViewModel(accounts, signOut)) { slug ->
    PublicProfileViewModel(slug, data, checkIns, accounts, visibility, snapshots)
}

class OwnProfileSnapshot internal constructor(own: OwnProfileState, public: PublicProfileState?, val active: Boolean) {
    val identity = own.profile.takeIf { active }
    val hasContent = active && own.hasContent
    val publicProfile = public?.profile?.takeIf { active && !it.isBlockedByViewer }
    val publicExpected = active && own.publicSlug != null
    val publicLoading = publicExpected && (public == null || public.isLoading)
    val publicFailed = publicExpected && public?.error != null
    val loading = active && own.isLoading
    val refreshing = active && own.isRefreshing
    val failed = active && own.error != null
    val refreshFailed = active && own.refreshError != null
    val checkIns = public?.checkIns.orEmpty().take(2).takeIf { publicProfile != null }.orEmpty().map(::OwnProfileCheckInSnapshot)
    val checkInsLoading = publicProfile != null && public?.isLoadingCheckIns == true
    val checkInsFailed = publicProfile != null && public?.checkInsError != null
    val followersOpen = publicProfile != null && public?.connectionKind == PublicProfileConnectionKind.Followers
    val followers = public?.connections.orEmpty().takeIf { followersOpen }.orEmpty()
    val followersLoading = followersOpen && (public?.isLoadingConnections == true || public?.isLoadingMoreConnections == true)
    val followersFailed = followersOpen && public?.connectionsError != null
    val moreFollowers = followersOpen && public?.hasMoreConnections == true
}

class OwnProfileCheckInSnapshot internal constructor(val checkIn: PublicProfileCheckIn) {
    private val sources = checkIn.profileCheckInPhotos()

    val originalPhotos = sources.originals
    val previewPhotos = sources.previews

    fun displayPhoto(index: Int) = sources.display(index)
    fun canOpenPhoto(index: Int) = sources.canOpen(index)
    fun drinkName(language: String) = checkIn.drink.profileCheckInDrink(language)
}

/** The root tab owns one session; an account replacement invalidates it even before UIKit swaps the tabs. */
class IosOwnProfileSession internal constructor(
    private val accounts: ProfileAccountPort,
    private val own: OwnProfileViewModel,
    private val publicFactory: (String) -> PublicProfileViewModel,
) {
    private val account = accounts.activeAccount()
    private val accountScope = accounts.cacheScope()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ownStore = ViewModelStore().apply { put("own-profile", own) }
    private val publicStore = ViewModelStore()
    private val updates = MutableStateFlow(0)
    private var public: PublicProfileViewModel? = null
    private var publicJob: Job? = null
    private var slug: String? = null
    private var previousIdentity = own.state.value.profile
    private var invalidated = false

    init {
        scope.launch { own.state.collect { synchronize(it) } }
        scope.launch { accounts.observeCacheScope().collect { if (!isCurrent()) invalidate() } }
        scope.launch { accounts.observeActiveAccount().collect { if (!isCurrent()) invalidate() } }
    }

    fun isCurrent() = !invalidated && account != null && accounts.activeAccount() == account && accounts.cacheScope() == accountScope

    fun current(): OwnProfileSnapshot = OwnProfileSnapshot(own.state.value, public?.state?.value, isCurrent())

    fun observe(onState: (OwnProfileSnapshot) -> Unit): Job = scope.launch { updates.collect { onState(current()) } }

    fun refresh() {
        if (!isCurrent()) return
        own.onAction(OwnProfileAction.Refresh)
        public?.onAction(PublicProfileAction.Refresh)
    }

    fun logout() {
        if (isCurrent()) own.onAction(OwnProfileAction.Logout)
    }

    fun retryCheckIns() = publicAction(PublicProfileAction.RetryCheckIns)
    fun openFollowers() = publicAction(PublicProfileAction.OpenConnections(PublicProfileConnectionKind.Followers))
    fun closeFollowers() = publicAction(PublicProfileAction.CloseConnections)
    fun retryFollowers() = publicAction(PublicProfileAction.RetryConnections)
    fun moreFollowers() = publicAction(PublicProfileAction.LoadMoreConnections)

    fun close() {
        invalidate()
        scope.cancel()
        ownStore.clear()
    }

    private fun publicAction(action: PublicProfileAction) {
        if (isCurrent()) public?.onAction(action)
    }

    private fun synchronize(state: OwnProfileState) {
        if (!isCurrent()) {
            invalidate()
            return
        }
        if (slug != state.publicSlug) replacePublic(state.publicSlug)
        else if (previousIdentity != state.profile) publicAction(PublicProfileAction.Refresh)
        previousIdentity = state.profile
        notify()
    }

    private fun replacePublic(next: String?) {
        publicJob?.cancel()
        publicStore.clear()
        slug = next
        public = next?.let(publicFactory)
        public?.let { model ->
            publicStore.put("profile-details", model)
            publicJob = scope.launch { model.state.collect { notify() } }
        }
    }

    private fun invalidate() {
        if (invalidated) return
        invalidated = true
        replacePublic(null)
        notify()
    }

    private fun notify() { updates.value += 1 }
}
