package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CpTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
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
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_back
import com.coffeepeek.feature.shop.impl.resources.shop_detail_error
import com.coffeepeek.feature.shop.impl.resources.shop_detail_retry
import com.coffeepeek.feature.shop.impl.resources.shop_detail_title
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCoffeeDetailsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopContactSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDescriptionSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopFeaturesSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopHeaderActions
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopRouteButton
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopMenuSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopOverviewHero
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopOverviewStats
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopReviewsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopScheduleSection
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.ShopDetailViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Runtime adapter delegates every effect to the caller; root navigation stays in the app. */
@Composable
internal fun ShopDetailScreen(
    viewModel: ShopDetailViewModel,
    onEvent: (ShopDetailEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(viewModel) {
        viewModel.events.collect(currentOnEvent)
    }
    ShopDetailScreenContent(state, viewModel::onAction)
}

/** Previewable composition; the Android route still uses the legacy detail screen. */
@Composable
internal fun ShopDetailScreenContent(
    state: ShopDetailState,
    onAction: (ShopDetailAction) -> Unit,
) {
    Scaffold(
        topBar = {
            CpTopBar(
                title = state.details?.overview?.title ?: stringResource(Res.string.shop_detail_title),
                backDescription = stringResource(Res.string.shop_detail_back),
                onBack = { onAction(ShopDetailAction.Back) },
                actions = {
                    if (state.details != null) {
                        ShopHeaderActions(
                            state.isFavorite, state.favoriteAvailable, state.isFavoriteLoading,
                            onSuggestChange = { onAction(ShopDetailAction.SuggestChange) },
                            onToggleFavorite = { onAction(ShopDetailAction.ToggleFavorite) },
                            onShare = { onAction(ShopDetailAction.Share) },
                        )
                    }
                },
            )
        },
        bottomBar = {
            val overview = state.details?.overview
            if (overview?.latitude != null && overview.longitude != null) {
                ShopRouteButton(
                    onClick = { onAction(ShopDetailAction.OpenRoute) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing4),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
            val details = state.details
            when {
                state.isLoading && details == null -> CircularProgressIndicator()
                details == null -> {
                    TextButton(onClick = { onAction(ShopDetailAction.Retry) }) {
                        Text(stringResource(Res.string.shop_detail_error) + " · " +
                            stringResource(Res.string.shop_detail_retry))
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = CpDimens.spacing6),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                ) {
                    item {
                        ShopOverviewHero(details.overview,
                            onOpenPhoto = { urls, index -> onAction(ShopDetailAction.OpenPhoto(urls, index)) },
                            onOpenMap = { onAction(ShopDetailAction.OpenMap) })
                    }
                    item { ShopOverviewStats(details.overview) }
                    details.overview.description?.let { description ->
                        item { ShopDescriptionSection(description, Modifier.padding(horizontal = CpDimens.spacing4)) }
                    }
                    details.menu?.takeIf { menu ->
                        menu.photos.isNotEmpty() || menu.items.any { it.availability.equals("Present", true) }
                    }?.let { menu ->
                        item { ShopMenuSection(menu, onOpenPhotos = {
                            onAction(ShopDetailAction.OpenMenuGallery)
                        }, modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    }
                    if (details.schedules.isNotEmpty()) {
                        item { ShopScheduleSection(details.schedules, state.todayDayOfWeek,
                            state.scheduleExpanded,
                            onToggle = { onAction(ShopDetailAction.ToggleSchedule) },
                            modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    }
                    item { ShopCoffeeDetailsSection(details.coffee,
                        onOpenRoaster = { onAction(ShopDetailAction.OpenRoaster(it)) },
                        modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    details.contact?.let { contact ->
                        item { ShopContactSection(contact,
                            onOpenLink = { onAction(ShopDetailAction.OpenLink(it)) },
                            onCopyPhone = { onAction(ShopDetailAction.CopyPhone(it)) },
                            modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    }
                    if (details.userCheckIns.isNotEmpty()) {
                        item { ShopCheckInsSection(details.userCheckIns,
                            onOpenPhoto = { urls, index -> onAction(ShopDetailAction.OpenPhoto(urls, index)) },
                            modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    }
                    item { ShopReviewsSection(details.reviews, details.overview.title,
                        state.isLoggedIn, state.currentUserId, state.pendingVoteIds,
                        onOpenPhoto = { urls, index -> onAction(ShopDetailAction.OpenPhoto(urls, index)) },
                        onVote = { onAction(ShopDetailAction.VoteHelpful(it)) },
                        onSignIn = { onAction(ShopDetailAction.SignIn) },
                        onRegister = { onAction(ShopDetailAction.Register) },
                        modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    if (details.features.isNotEmpty()) {
                        item { ShopFeaturesSection(details.features, state.featuresExpanded,
                            onToggle = { onAction(ShopDetailAction.ToggleFeatures) },
                            modifier = Modifier.padding(horizontal = CpDimens.spacing4)) }
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun ShopDetailScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailScreenContent(previewState(), onAction = {})
}

@Preview @Composable private fun ShopDetailScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailScreenContent(previewState().copy(scheduleExpanded = true, featuresExpanded = true), onAction = {})
}

private fun previewState(): ShopDetailState {
    val overview = ShopOverview("shop-1", "Кофейня", "Уютная кофейня с собственной обжаркой.",
        "Минск, улица Ленина, 1", 53.9, 27.5, 4.6, 12, true, emptyList())
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
        ),
        isLoading = false,
        isLoggedIn = true,
        currentUserId = "user-2",
        todayDayOfWeek = 1,
    )
}
