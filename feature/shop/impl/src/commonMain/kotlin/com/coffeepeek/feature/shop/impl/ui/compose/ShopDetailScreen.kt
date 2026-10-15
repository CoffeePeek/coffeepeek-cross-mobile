package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopContact
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.domain.model.ShopMenu
import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.domain.model.ShopRoaster
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.domain.model.ScheduleInterval
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCoffeeDetailsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopContactSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDescriptionSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopFeaturesSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDetailTopActions
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDetailLoadState
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDetailRefreshStatus
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDetailBottomBar
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopOverviewHeader
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPriceInfoSheet
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopReportIssueSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopMenuSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopOverviewHero
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopOverviewStats
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPublicCheckInsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopScheduleSection
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopPriceLevel
import com.coffeepeek.feature.shop.impl.ui.ShopDetailViewModel
import com.coffeepeek.feature.shop.impl.ui.data.formatShopMenuDate
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_favorite_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_vote_error
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Runtime adapter delegates every effect to the caller; root navigation stays in the app. */
@Composable
internal fun ShopDetailScreen(
    viewModel: ShopDetailViewModel,
    onEvent: (ShopDetailEvent) -> Unit,
    sessionGeneration: Int,
    formatDistance: (Double, Double) -> String? = { _, _ -> null },
    forCheckIn: Boolean = false,
    formatCheckInDate: (String) -> String = { it.take(10) },
    formatMenuDate: (String) -> String = ::formatShopMenuDate,
    refreshGeneration: Long = 0,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentGeneration by rememberUpdatedState(sessionGeneration)
    val favoriteError by rememberUpdatedState(stringResource(Res.string.shop_detail_favorite_error))
    val voteError by rememberUpdatedState(stringResource(Res.string.shop_checkin_list_vote_error))
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, sessionGeneration) {
        viewModel.events.collect { envelope ->
            if (envelope.sessionGeneration != currentGeneration) return@collect
            when (val event = envelope.event) {
                ShopDetailEvent.FavoriteFailed -> launch { snackbarHostState.showSnackbar(favoriteError) }
                ShopDetailEvent.VoteFailed -> launch { snackbarHostState.showSnackbar(voteError) }
                else -> currentOnEvent(event)
            }
        }
    }
    LaunchedEffect(viewModel, sessionGeneration) {
        viewModel.onAction(ShopDetailAction.SessionChanged(sessionGeneration))
    }
    LaunchedEffect(viewModel, refreshGeneration) {
        viewModel.onAction(ShopDetailAction.Refresh(refreshGeneration))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(ShopDetailAction.Resume) }
    val visibleState = state.takeIf { it.sessionGeneration == sessionGeneration }
        ?: ShopDetailState(sessionGeneration = sessionGeneration)
    val overview = visibleState.details?.overview
    val latitude = overview?.latitude
    val longitude = overview?.longitude
    val distance = if (latitude != null && longitude != null)
        formatDistance(latitude, longitude) else null
    key(sessionGeneration) {
        ShopDetailScreenContent(visibleState, viewModel::onAction, distance, forCheckIn,
            formatCheckInDate, formatMenuDate, snackbarHostState)
    }
}

/** Previewable composition; app-owned platform renderers select the entry without feature navigation/DI lookups. */
@Composable
internal fun ShopDetailScreenContent(
    state: ShopDetailState,
    onAction: (ShopDetailAction) -> Unit,
    distanceLabel: String? = null,
    forCheckIn: Boolean = false,
    formatCheckInDate: (String) -> String = { it.take(10) },
    formatMenuDate: (String) -> String = ::formatShopMenuDate,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val priceLevel = ShopPriceLevel.from(state.details?.overview?.priceRange)
    if (state.priceInfoVisible && priceLevel != null) {
        ShopPriceInfoSheet(priceLevel, onDismiss = { onAction(ShopDetailAction.DismissPriceInfo) })
    }
    val hazeState = rememberHazeState()
    val floatingActionsClearance = 72.dp
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
      Scaffold(
        modifier = Modifier.hazeSource(hazeState),
        snackbarHost = { SnackbarHost(snackbarHostState,
            Modifier.navigationBarsPadding().padding(bottom = floatingActionsClearance)) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets), contentAlignment = Alignment.Center) {
            val details = state.details
            when {
                details == null -> ShopDetailLoadState(state.isLoading,
                    onRetry = { onAction(ShopDetailAction.Retry) })
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = floatingActionsClearance +
                        CoffeePeekDimens.spacing4 + CoffeePeekDimens.spacing6),
                ) {
                    item {
                        Column {
                            ShopOverviewHero(details.overview,
                                onOpenPhoto = { urls, index -> onAction(ShopDetailAction.OpenPhoto(urls, index)) })
                            Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing4,
                                vertical = CoffeePeekDimens.spacing3),
                                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                                ShopOverviewHeader(details.overview,
                                    onOpenMap = { onAction(ShopDetailAction.OpenMap) })
                                ShopOverviewStats(details.overview,
                                    onOpenCheckIns = { onAction(ShopDetailAction.OpenCheckIns) },
                                    onOpenPriceInfo = { onAction(ShopDetailAction.OpenPriceInfo) },
                                    distanceLabel = distanceLabel)
                            }
                        }
                    }
                    if (state.isLoading || state.hasError) {
                        item { ShopDetailRefreshStatus(state.isLoading,
                            onRetry = { onAction(ShopDetailAction.Retry) },
                            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4,
                                vertical = CoffeePeekDimens.spacing3)) }
                    }
                    details.overview.description?.let { description ->
                        item { ShopDescriptionSection(description, Modifier.padding(horizontal = CoffeePeekDimens.spacing4,
                            vertical = CoffeePeekDimens.spacing2)) }
                    }
                    item { ShopCoffeeDetailsSection(details.coffee,
                        onOpenRoaster = { onAction(ShopDetailAction.OpenRoaster(it)) },
                        modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = 6.dp)) }
                    if (details.schedules.isNotEmpty()) {
                        item { ShopScheduleSection(details.schedules, state.todayDayOfWeek,
                            state.scheduleExpanded,
                            onToggle = { onAction(ShopDetailAction.ToggleSchedule) },
                            isOpen = details.overview.isOpen,
                            minuteOfDay = state.todayMinuteOfDay,
                            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4,
                                vertical = CoffeePeekDimens.spacing3)) }
                    }
                    details.menu?.takeIf { menu ->
                        menu.photos.isNotEmpty() || menu.items.any { it.availability.equals("Present", true) }
                    }?.let { menu ->
                        item { ShopMenuSection(menu, onOpenPhotos = {
                            onAction(ShopDetailAction.OpenMenuGallery)
                        }, onOpenPhoto = { index ->
                            onAction(ShopDetailAction.OpenPhoto(menu.photos.map { it.fullUrl }, index))
                        }, modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = 6.dp),
                            formatDate = formatMenuDate) }
                    }
                    details.contact?.let { contact ->
                        item { ShopContactSection(contact,
                            onOpenLink = { onAction(ShopDetailAction.OpenLink(it)) },
                            onCopyPhone = { onAction(ShopDetailAction.CopyPhone(it)) },
                            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4,
                                vertical = CoffeePeekDimens.spacing4)) }
                    }
                    if (details.features.isNotEmpty()) {
                        item { ShopFeaturesSection(details.features, state.featuresExpanded,
                            onToggle = { onAction(ShopDetailAction.ToggleFeatures) },
                            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4)
                                .padding(top = CoffeePeekDimens.spacing6, bottom = CoffeePeekDimens.spacing3)) }
                    }
                    item { ShopPublicCheckInsSection(details.checkIns, details.overview.title,
                        state.isLoggedIn, state.currentUserId, details.userCheckIns.map { it.id }.toSet(),
                        state.pendingCheckInVoteId,
                        onOpenPhoto = { urls, index -> onAction(ShopDetailAction.OpenPhoto(urls, index)) },
                        onVote = { onAction(ShopDetailAction.VoteCheckInHelpful(it)) },
                        onReport = { onAction(ShopDetailAction.ReportCheckIn(it)) },
                        onOpenAll = { onAction(ShopDetailAction.OpenCheckIns) },
                        onSignIn = { onAction(ShopDetailAction.SignIn) },
                        onRegister = { onAction(ShopDetailAction.Register) },
                        overallRating = details.overview.rating,
                        checkInCount = details.overview.reviewCount,
                        formatDate = formatCheckInDate,
                        isRefreshing = state.isLoading,
                        modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4)
                            .padding(top = CoffeePeekDimens.spacing3, bottom = CoffeePeekDimens.spacing2)) }
                    item { ShopReportIssueSection(onReport = { onAction(ShopDetailAction.ReportShop) },
                        modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4,
                            vertical = CoffeePeekDimens.spacing2)) }
                }
            }
        }
      }
      ShopDetailTopActions(
          showShopActions = state.details != null,
          isFavorite = state.isFavorite,
          favoriteAvailable = state.favoriteAvailable,
          isFavoriteLoading = state.isFavoriteLoading,
          onBack = { onAction(ShopDetailAction.Back) },
          onSuggestChange = { onAction(ShopDetailAction.SuggestChange) },
          onToggleFavorite = { onAction(ShopDetailAction.ToggleFavorite) },
          onShare = { onAction(ShopDetailAction.Share) },
          hazeState = hazeState,
      )
      state.details?.overview?.let { overview ->
          ShopDetailBottomBar(
              canOpenRoute = overview.latitude != null && overview.longitude != null,
              forCheckIn = forCheckIn,
              onRoute = { onAction(ShopDetailAction.OpenRoute) },
              onCheckIn = { onAction(ShopDetailAction.OpenCheckIn) },
              onChooseAnotherShop = { onAction(ShopDetailAction.ChooseAnotherShop) },
              modifier = Modifier.align(Alignment.BottomCenter),
          )
      }
    }
}

@Preview @Composable private fun ShopDetailScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailScreenContent(previewState(), onAction = {}, distanceLabel = "350 м")
}

@Preview @Composable private fun ShopDetailScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailScreenContent(previewState().copy(scheduleExpanded = true, featuresExpanded = true),
        onAction = {}, distanceLabel = "350 м")
}

@Preview @Composable private fun ShopDetailPickerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailScreenContent(previewState(), onAction = {}, forCheckIn = true)
}

@Preview @Composable private fun ShopDetailPickerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailScreenContent(previewState(), onAction = {}, forCheckIn = true)
}

@Preview @Composable private fun ShopDetailLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailScreenContent(ShopDetailState(), {})
}
@Preview @Composable private fun ShopDetailLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailScreenContent(ShopDetailState(), {})
}
@Preview @Composable private fun ShopDetailErrorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailScreenContent(ShopDetailState(isLoading = false, hasError = true), {})
}
@Preview @Composable private fun ShopDetailErrorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailScreenContent(ShopDetailState(isLoading = false, hasError = true), {})
}
@Preview @Composable private fun ShopDetailRefreshErrorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailScreenContent(previewState().copy(hasError = true), {}, distanceLabel = "350 м")
}
@Preview @Composable private fun ShopDetailRefreshErrorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailScreenContent(previewState().copy(hasError = true), {}, distanceLabel = "350 м")
}

private fun previewState(): ShopDetailState {
    val overview = ShopOverview("shop-1", "Кофейня", "Уютная кофейня с собственной обжаркой.",
        "Минск, улица Ленина, 1", 53.9, 27.5, 4.6, 12, true, emptyList(), priceRange = "$$")
    return ShopDetailState(
        details = ShopDetails(
            overview = overview,
            menu = ShopMenu(items = listOf(ShopMenuItem("flat-white", "Флэт уайт", "Flat white",
                "Espresso", "Present", 5.5, "BYN", 250))),
            schedules = listOf(ShopSchedule(1, false, listOf(ScheduleInterval("09:00", "21:00")))),
            coffee = ShopCoffeeDetails(beans = listOf("Эфиопия"),
                roasters = listOf(ShopRoaster("roaster-1", "Местная обжарка", null)),
                equipment = listOf("V60")),
            contact = ShopContact(phone = "+375 29 123 45 67", website = "coffeepeek.app"),
            features = listOf(ShopFeature("Wi-Fi", "wifi", false)),
            reviews = listOf(ShopReview("review-1", null, "user-1", "shop-1", "Алексей",
                "Отличный кофе", "Хорошая атмосфера", ShopRating(5, 4, 5),
                "2026-10-01T12:00:00Z", emptyList(), 3, false)),
            userCheckIns = listOf(ShopCheckIn("check-in-1", "user-1", "shop-1", "Вкусный фильтр",
                "2026-10-01", "2026-10-01", null, emptyList(), emptyList(), null)),
            checkIns = listOf(ShopCheckIn("public-check-in", "user-1", "shop-1", "Хорошая атмосфера",
                "2026-10-01", "2026-10-01", null, emptyList(), emptyList(), ShopRating(5, 4, 5),
                username = "Алексей", helpfulCount = 3)),
        ),
        isLoading = false,
        isLoggedIn = true,
        currentUserId = "user-2",
        todayDayOfWeek = 1,
        todayMinuteOfDay = 12 * 60,
    )
}
