package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.CheckInKind
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.analytics.CheckInStage
import com.coffeepeek.admin.analytics.CheckInStep
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.analytics.safeAnalyticsError
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.api.EditField
import com.coffeepeek.feature.community.api.FeedAction
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.model.PostVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The account key is the user id (else the token) of an active session, null for a guest — the key the legacy feed used. */
class SessionCommunityAccount(private val sessions: SessionReader, private val accounts: ProfileAccountPort) : CommunityAccount {
    private fun key(session: Session?): String? = if (session != null && sessions.isActiveSession(session)) session.userId ?: session.accessToken else null
    override val accountKey: Flow<String?> = sessions.observeSession().map(::key)
    override fun currentAccountKey(): String? = key(sessions.peekSession())
    override suspend fun ownAuthorSlug(): Result<String?> = accounts.loadOwnProfile(forceRefresh = true).map { it.address?.slug?.takeIf(String::isNotBlank) }
}

class ShopCommunityDrinks(private val shops: ShopCheckInRepository) : ConsumedDrinks {
    override suspend fun load(): Result<List<DrinkOption>> = shops.getDrinkOptions().map { drinks -> drinks.map { DrinkOption(it.slug, it.nameRu, it.nameEn) } }
}

/** The feed and edit events of the legacy screen, with the same names and properties. */
class AppCommunityAnalytics : CommunityAnalytics {
    private var tracker: CheckInTracker? = null

    override fun feedAction(action: FeedAction, trigger: FeedTrigger) {
        Analytics.track(
            AnalyticsEvent.FEED_ACTION_CLICKED,
            "action" to when (action) { FeedAction.Refresh -> "refresh"; FeedAction.LoadMore -> "load_more"; FeedAction.Retry -> "retry" },
            "trigger" to when (trigger) { FeedTrigger.Gesture -> "gesture"; FeedTrigger.Scroll -> "scroll"; FeedTrigger.Button -> "button" },
        )
    }

    override fun itemInteracted(action: FeedItemAction, isHome: Boolean) {
        Analytics.track(
            AnalyticsEvent.FEED_ITEM_INTERACTED,
            "action" to when (action) {
                FeedItemAction.Open -> "open"
                FeedItemAction.Shop -> "shop"
                FeedItemAction.Photo -> "photo"
                FeedItemAction.Like -> "like"
                FeedItemAction.Unlike -> "unlike"
                FeedItemAction.Share -> "share"
                FeedItemAction.Coffee -> "coffee"
            },
            "check_in_kind" to if (isHome) "home" else "shop",
        )
    }

    override fun likeGateShown() { Analytics.gate("feed_like") }

    override fun editOpened() { tracker = CheckInTracker(CheckInKind.Visit, CheckInSource.Diary, edit = true).also { it.step(CheckInStep.Editor) } }
    override fun editFieldChanged(field: EditField) { tracker?.field(when (field) { EditField.Note -> "note"; EditField.Rating -> "rating"; EditField.Drink -> "drink" }) }
    override fun editSubmitted() { tracker?.submit() }
    override fun editRejected() { tracker?.validation(listOf("form")) }
    override fun editSaved(visibility: PostVisibility, hasPhotos: Boolean) { tracker?.saved(visibility.name, hasPhotos) }
    override fun editFailed(error: Throwable) { tracker?.failed(CheckInStage.Save, safeAnalyticsError(error)) }
    override fun editCancelled() { tracker?.cancel("close") }
}
