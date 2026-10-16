package com.coffeepeek.admin.analytics

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay

internal val LocalAnalyticsVisible = staticCompositionLocalOf { true }
internal val LocalAnalyticsUnobscured = staticCompositionLocalOf { true }
private val LocalAnalyticsOverlay = staticCompositionLocalOf<String?> { null }
private val exposures = FeatureExposureLedger()

/** A name comes only from the route's class, never from serialized route arguments. */
internal fun analyticsPage(route: String?): String = when (route?.substringBefore('?')?.substringBefore('/')?.substringAfterLast('.')) {
    "Auth", "ForgotPassword", "ResetPassword" -> "login"
    "Register" -> "register"
    "FeedTab", "FeedGraph" -> "discovery"
    "CoffeeTab", "CoffeeGraph" -> "coffees"
    "CommunityTab", "CommunityGraph" -> "feed"
    "ProfileTab", "ProfileGraph" -> "profile"
    "UserProfile" -> "user_profile"
    "SettingsTab", "SettingsGraph", "CitySettings", "ThemeSettings" -> "settings"
    "UsagePolicy" -> "privacy"
    "ShopDetail", "ShopMenuGallery", "ShopReviews" -> "shop_detail"
    "CoffeeDetail" -> "coffee_detail"
    "RoasterDetail" -> "roaster_detail"
    "CreateCheckIn" -> "check_in_create"
    "EditHomeCheckIn" -> "check_in_edit"
    "VisitedPlaces", "HomeCheckIns" -> "my_check_ins"
    "EditProfile", "DeleteAccountPending", "Favorites" -> "profile"
    "MyContributions", "ShopChangeRequestDetail" -> "contributions"
    "SuggestShopChange", "ShopChangeEditor", "AddShop" -> "shop_edit"
    else -> "other"
}

@Composable
internal fun TrackNavigationScreen(entry: NavBackStackEntry?) {
    val owner = entry ?: return
    // Main is a retained container; its visible tab owns the actual screen event.
    if (owner.destination.route?.substringBefore('?')?.substringBefore('/')?.substringAfterLast('.') == "Main") return
    val state by owner.lifecycle.currentStateFlow.collectAsState()
    val page = analyticsPage(owner.destination.route)
    LaunchedEffect(owner.id, state) {
        if (state == Lifecycle.State.RESUMED) Analytics.service?.screenShown(page, owner.id)
    }
}

@Composable
internal fun TrackEntityDetail(entity: Entity, loaded: Boolean) {
    val service = Analytics.service ?: return
    val state by service.state.collectAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val visible = LocalAnalyticsVisible.current
    val page = when (entity) { Entity.Shop -> "shop_detail"; Entity.Coffee -> "coffee_detail"; Entity.Roaster -> "roaster_detail" }
    var sent by remember { mutableStateOf("") }
    val entry = remember { DetailEntryMemory.consume(entity) }
    LaunchedEffect(loaded, state.ready, state.visit, state.generation, visible, lifecycle) {
        val key = "${state.generation}:${state.visit}:${state.visitSequence}"
        if (loaded && state.ready && visible && lifecycle == Lifecycle.State.RESUMED && state.page == page && key != sent &&
            service.track(AnalyticsEvent.ENTITY_DETAIL_VIEWED, entry)) sent = key
    }
}

@Composable
internal fun TrackFeedViewed(firstPageCount: Int?, public: Boolean) {
    val service = Analytics.service ?: return
    val state by service.state.collectAsState()
    val visible = LocalAnalyticsVisible.current
    var sent by remember { mutableStateOf("") }
    LaunchedEffect(state.ready, state.visit, state.generation, firstPageCount, visible, public) {
        val key = "${state.generation}:${state.visit}:${state.visitSequence}"
        if (visible && public && state.ready && state.page == "feed" && firstPageCount != null && sent != key &&
            service.track(AnalyticsEvent.FEED_VIEWED, mapOf("result_count" to firstPageCount, "is_empty" to (firstPageCount == 0)))) sent = key
    }
}

/** Actual clipped area + uninterrupted 500 ms + foreground + usable screen. */
internal fun Modifier.analyticsExposure(feature: String, placement: String, page: String): Modifier = composed {
    val service = Analytics.service ?: return@composed this
    val state by service.state.collectAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val visible = LocalAnalyticsVisible.current
    val unobscured = LocalAnalyticsUnobscured.current
    val overlay = LocalAnalyticsOverlay.current
    val window = LocalWindowInfo.current
    var fraction by remember { mutableFloatStateOf(0f) }
    val enoughVisible by remember { derivedStateOf { fraction >= .1f } }
    val eligible = enoughVisible && visible && unobscured && (state.overlays.isEmpty() || state.overlays.lastOrNull() == overlay) && state.ready && state.foreground &&
        lifecycle == Lifecycle.State.RESUMED && state.page == page
    LaunchedEffect(eligible, state.generation, state.visit, state.visitSequence) {
        if (eligible) {
            delay(500)
            val visit = "${state.generation}:${state.visit}:${state.visitSequence}"
            exposures.report(visit, feature, placement, fraction, state.foreground, unobscured, 500) {
                service.track(AnalyticsEvent.FEATURE_EXPOSED, mapOf("feature" to feature, "placement" to placement))
            }
        }
    }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val clippedWidth = (minOf(bounds.right, window.containerSize.width.toFloat()) - maxOf(bounds.left, 0f)).coerceAtLeast(0f)
        val clippedHeight = (minOf(bounds.bottom, window.containerSize.height.toFloat()) - maxOf(bounds.top, 0f)).coerceAtLeast(0f)
        val area = coordinates.size.width.toFloat() * coordinates.size.height
        fraction = if (area > 0) (clippedWidth * clippedHeight / area).coerceIn(0f, 1f) else 0f
    }
}

@Composable
internal fun AnalyticsOverlay(content: @Composable () -> Unit) {
    val id = remember { correlationId("overlay") }
    val visible = LocalAnalyticsVisible.current
    DisposableEffect(id, visible) {
        if (visible) Analytics.service?.overlay(id, true)
        onDispose { Analytics.service?.overlay(id, false) }
    }
    CompositionLocalProvider(LocalAnalyticsOverlay provides id) { content() }
}

@Composable
internal fun AnalyticsOcclusion() {
    val id = remember { correlationId("overlay") }
    DisposableEffect(id) {
        Analytics.service?.overlay(id, true)
        onDispose { Analytics.service?.overlay(id, false) }
    }
}
