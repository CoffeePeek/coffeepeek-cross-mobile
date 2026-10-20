package com.coffeepeek.feature.profile.api

import com.coffeepeek.feature.profile.domain.ProfileAddress
import com.coffeepeek.feature.profile.domain.PublicProfileCheckInPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The signed-in user's own account as the profile screens need it. Owned by the app (session and cache guards). */
data class OwnProfile(
    val userName: String,
    val email: String,
    val about: String?,
    val avatarUrl: String?,
    val checkInCount: Int,
    val addedShopsCount: Int,
    val addedRoastersCount: Int,
    val followersCount: Int,
    val address: ProfileAddress?,
)

/**
 * Active account and its cached profile. An account key identifies a signed-in session (user id, else the access token);
 * `null` means a guest or an inactive session. Results of a request started for one account must be dropped by the caller
 * when [observeActiveAccount] has moved on.
 */
interface ProfileAccountPort {
    fun activeAccount(): String?

    /** Distinct account keys, replaying the current one. */
    fun observeActiveAccount(): Flow<String?>

    fun cachedOwnProfile(): OwnProfile?

    /** The shared in-memory profile; `null` while signed out or not loaded yet. */
    fun observeOwnProfile(): Flow<OwnProfile?>

    /** [forceRefresh] always asks the server; otherwise the cache is returned when present. */
    suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile>
}

/** Which authors the viewer has blocked. [hides] is also true while the list is not ready, so nothing leaks before it loads. */
data class ProfileAuthorVisibilityState(
    val revision: Long = 0,
    val ready: Boolean = true,
    val blockedIds: Set<String> = emptySet(),
    val blockedSlugs: Set<String> = emptySet(),
) {
    fun hides(authorId: String?, slug: String?): Boolean = !ready ||
        (authorId != null && authorId in blockedIds) || (slug != null && slug in blockedSlugs)
}

interface ProfileAuthorVisibility {
    fun current(): ProfileAuthorVisibilityState

    /** Replays the current state, then every change. A new [ProfileAuthorVisibilityState.revision] invalidates personalized pages. */
    fun observe(): Flow<ProfileAuthorVisibilityState>
    suspend fun refresh(): Result<Unit>

    object None : ProfileAuthorVisibility {
        private val state = ProfileAuthorVisibilityState()
        override fun current() = state
        override fun observe(): Flow<ProfileAuthorVisibilityState> = flowOf(state)
        override suspend fun refresh() = Result.success(Unit)
    }
}

/** Profile-owned analytics. The legacy owner emitted one event; screen views come from the app's route mapping. */
interface ProfileAnalyticsPort {
    /** The "all favorites" action on the own profile: exactly one `favorites_opened {entry_point: profile}`. */
    fun favoritesOpened()

    object None : ProfileAnalyticsPort {
        override fun favoritesOpened() = Unit
    }
}

/**
 * The author's public check-ins, filtered to public and approved ones for exactly this author. Supplied by the app until the timeline
 * owner exposes its own API; the app keeps whatever rendering data it needs for these ids.
 */
interface PublicCheckInsPort {
    suspend fun page(slug: String, cursor: String?): Result<PublicProfileCheckInPage>
}

/** Opaque public identity of an author. The slug is only a fallback for older content. */
data class ProfileAuthor(val id: String?, val slug: String?, val name: String, val avatarUrl: String? = null)

/** Block and unblock confirmations are owned by the app; the profile only asks. */
interface ProfileAuthorActions {
    fun requestBlock(author: ProfileAuthor)
    fun requestUnblock(author: ProfileAuthor)

    object None : ProfileAuthorActions {
        override fun requestBlock(author: ProfileAuthor) = Unit
        override fun requestUnblock(author: ProfileAuthor) = Unit
    }
}

/**
 * Ends the signed-in account. The app owns the order and the side effects (server logout, platform sign-out, local drafts) and
 * surfaces its own failures; the profile only resets its screen first and then calls this.
 */
interface ProfileSignOutPort {
    suspend fun signOut()

    object None : ProfileSignOutPort {
        override suspend fun signOut() = Unit
    }
}
