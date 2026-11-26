package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.analytics.NativeScreenAnalytics
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.notifications.IosNotificationInbox
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.NavMetrics
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.notifications.NotificationsRenderer
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.image.FullScreenPhotoPresence
import com.coffeepeek.core.media.prefetchPhotoWindow
import com.coffeepeek.core.media.upcomingPhotoUrls
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.NativeCommunityFeed
import com.coffeepeek.feature.community.impl.NativeCommunityPost
import com.coffeepeek.feature.community.impl.NativeCommunityFeedSnapshot
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.impl.createIosNotificationInboxSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal class IosNativeCommunityFactory(private val repository: CommunityRepository, private val account: CommunityAccount,
    private val analytics: CommunityAnalytics, private val drinks: ConsumedDrinks, private val authors: AuthorVisibility,
    private val saved: CommunitySavedPosts, private val blocks: AuthorBlockActions, private val sessions: SessionReader,
    private val language: LanguagePort, private val fetcher: ImageUrlFetcher, private val notifications: NotificationFeature,
    private val notificationRoutes: NotificationsRenderer,
) {
    fun feed(registerRefresh: (() -> Unit) -> (() -> Unit)): UIViewController {
        val media = IosCatalogueMedia(fetcher, Constants.BASE_URL)
        val source = IosCommunityFeed(null, CommunityFeedActions(emptyMap(), emptyMap()), media, sessions)
        val environment = IosCommunityEnvironment(source.scope, sessions, language, fetcher)
        val core = NativeCommunityFeed(repository, account, analytics, drinks, authors, saved, blocks, environment.host, feedNavigation())
        NativeCommunityFeedBinding(source, core, environment, media, registerRefresh)
        source.scope.launch { language.observe().collect { selection ->
            val session = createIosNotificationInboxSession(notifications, notificationRoutes::open, ::signIn)
            source.bindNotifications(IosNotificationInbox(session), selection.languageTag)
        } }
        return communityFeedController(source)
    }

    fun post(id: String): UIViewController {
        val source = IosCheckInDetail(NativeCheckInDetailPresentation(null, "", ""), CommunityCardActions(emptyMap(), {}),
            IosCatalogueMedia(fetcher, Constants.BASE_URL), sessions)
        val environment = IosCommunityEnvironment(source.scope, sessions, language, fetcher)
        val core = NativeCommunityPost(id, repository, account, authors, analytics, drinks, blocks, environment.host, postNavigation())
        var closed = false
        source.bindOwner(environment.overlays, core::visibility) {
            if (!closed) {
                closed = true
                environment.close()
                core.close()
            }
        }
        environment.observeAccount(source::close)
        source.scope.launch { core.active.collect { if (!it) source.close() } }
        source.scope.launch { core.state.filterNotNull().collect { snapshot ->
            source.update(snapshot.data.nativeDetail(), snapshot.actions)
            environment.update(snapshot.sheet, snapshot.photo, 0.0)
        } }
        return communityPostController(source)
    }
}

private class NativeCommunityFeedBinding(private val source: IosCommunityFeed, private val core: NativeCommunityFeed,
    private val environment: IosCommunityEnvironment, private val media: IosCatalogueMedia, registerRefresh: (() -> Unit) -> (() -> Unit),
) {
    private val job = SupervisorJob(source.scope.coroutineContext[Job])
    private val scope = CoroutineScope(source.scope.coroutineContext + job)
    private var clearance = NavMetrics.iosTabBarClearanceFloor.value.toDouble()
    private var closed = false
    private val releaseRefresh = registerRefresh(core::refresh)

    init {
        source.bindOwner(environment.overlays, ::visible, ::clearance, ::close)
        environment.observeAccount(source::close)
        scope.launch { core.active.collect { if (!it) source.close() } }
        scope.launch { core.state.filterNotNull().collect { snapshot ->
            source.update(snapshot.data, snapshot.actions)
            environment.update(snapshot.sheet, snapshot.photo, clearance)
        } }
        analytics()
        prefetch()
    }
    private fun visible(shown: Boolean) {
        core.presentation(shown, clearance)
        if (shown) core.resumed()
    }
    private fun clearance(value: Double) {
        clearance = maxOf(value, NavMetrics.iosTabBarClearanceFloor.value.toDouble())
        core.presentation(source.screenVisible.value, clearance)
    }
    private fun analytics() {
        val usable = combine(source.screenVisible, core.state) { shown, state -> shown && state?.data?.interactive == true }
        val tracker = NativeScreenAnalytics(scope, "feed", usable)
        tracker.feedViewed(core.state.map { it?.firstPageCount to (it?.publicTimeline == true) })
        tracker.feature("feed", "feed", combine(source.visibleFractions, core.state) { fractions, state ->
            (fractions["feed"] ?: 0.0) >= .1 && state?.data?.loading == false
        })
        listOf(false, true).forEach { home -> tracker.feature(if (home) "feed_home_check_in" else "feed_shop_check_in", "feed",
            combine(source.visibleFractions, core.state) { fractions, state -> state?.data?.cards?.any {
                it.body.isHome == home && it.key != state.data.teaserKey && (fractions[it.key] ?: 0.0) >= .1
            } == true }) }
    }
    private fun prefetch() {
        scope.launch { combine(source.visibleFractions, core.state, source.screenVisible, FullScreenPhotoPresence.activeFlow) { fractions, state, shown, photo ->
            if (!shown || photo || state?.data?.interactive != true) emptyList() else upcoming(state, fractions)
        }.distinctUntilChanged().collectLatest { urls ->
            prefetchPhotoWindow(urls) { if (environment.isCurrent()) media.prefetch(it, environment::isCurrent) }
        } }
    }
    private fun upcoming(snapshot: NativeCommunityFeedSnapshot, fractions: Map<String, Double>) = upcomingPhotoUrls(
        snapshot.data.cards.map { card -> card.key to card.body.photos.take(3).map { it.previewUrl } },
        fractions.filterValues { it >= .1 }.keys.toList(), atStart = false)
    private fun close() {
        if (closed) return
        closed = true
        job.cancel()
        environment.close()
        core.close()
        releaseRefresh()
    }
}

private fun CommunityPostPresentation.nativeDetail() = NativeCheckInDetailPresentation(card, title, backLabel,
    loadingLabel = loadingLabel, errorText = listOfNotNull(errorTitle, errorText).joinToString("\n").takeIf(String::isNotBlank), retryLabel = retryLabel)

private fun signIn() = Navigator.navigate(Navigator.Screen.Auth)
private fun feedNavigation() = CommunityNavigation(::signIn,
    { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
    { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }, { Navigator.navigate(Navigator.Screen.EditHomeCheckIn(it)) },
    { Navigator.navigate(Navigator.Screen.CheckInDetail(it)) }, { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) })
private fun postNavigation() = CommunityPostNavigation(Navigator::popBack, ::signIn,
    { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
    { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }, { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
    { Navigator.navigate(Navigator.Screen.EditHomeCheckIn(it)) })

private fun NativeCommunityPost.visibility(shown: Boolean) {
    presentation(shown)
    if (shown) resumed()
}

@Composable
internal fun iosNativeCommunityContent(registerRefresh: (() -> Unit) -> (() -> Unit)): (Navigator.Screen) -> (() -> UIViewController)? {
    val repository = koinInject<CommunityRepository>()
    val account = koinInject<CommunityAccount>()
    val analytics = koinInject<CommunityAnalytics>()
    val drinks = koinInject<ConsumedDrinks>()
    val authors = koinInject<AuthorVisibility>()
    val saved = koinInject<CommunitySavedPosts>()
    val blocks = koinInject<AuthorBlockActions>()
    val sessions = koinInject<SessionReader>()
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val notifications = koinInject<NotificationFeature>()
    val routes = koinInject<NotificationsRenderer>()
    val current = remember(repository, account, analytics, drinks, authors, saved, blocks, sessions, language, fetcher, notifications, routes) {
        IosNativeCommunityFactory(repository, account, analytics, drinks, authors, saved, blocks, sessions, language, fetcher, notifications, routes)
    }
    return { route -> when (route) {
        Navigator.Screen.CommunityTab -> { { current.feed(registerRefresh) } }
        is Navigator.Screen.CheckInDetail -> { { current.post(route.id) } }
        else -> null
    } }
}
