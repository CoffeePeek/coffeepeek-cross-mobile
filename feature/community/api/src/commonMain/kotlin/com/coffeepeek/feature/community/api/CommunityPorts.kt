package com.coffeepeek.feature.community.api

import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.model.PostVisibility
import kotlinx.coroutines.flow.Flow

/** Who is signed in, as the timeline needs to know it. The application backs it with its session. */
interface CommunityAccount {
    /** The stable key of the active account (its user id, else its token), null for a guest. A replaced account is a new key. */
    val accountKey: Flow<String?>

    /** The key of the account that is active right now; a late answer for a previous key must be dropped. */
    fun currentAccountKey(): String?

    /** The canonical slug of the signed-in user's own profile, used to recognise own posts in the public timeline. */
    suspend fun ownAuthorSlug(): Result<String?>
}

/** The drinks a post can be edited to; the application owns the catalog. */
interface ConsumedDrinks {
    suspend fun load(): Result<List<DrinkOption>>
}

enum class FeedAction { Refresh, LoadMore, Retry }

enum class FeedTrigger { Gesture, Scroll, Button }

enum class FeedItemAction { Shop, Photo, Like, Unlike }

enum class EditField { Note, Rating, Drink }

/** The events of the timeline, with the names and properties of the legacy screen; the application maps them to its SDK. */
interface CommunityAnalytics {
    /** Only the public timeline reports feed events; the caller checks that. */
    fun feedAction(action: FeedAction, trigger: FeedTrigger)
    fun itemInteracted(action: FeedItemAction, isHome: Boolean)

    /** A guest tried to like a post. */
    fun likeGateShown()

    fun editOpened()
    fun editFieldChanged(field: EditField)
    fun editSubmitted()
    fun editRejected()
    fun editSaved(visibility: PostVisibility, hasPhotos: Boolean)
    fun editFailed(error: Throwable)
    fun editCancelled()
}
