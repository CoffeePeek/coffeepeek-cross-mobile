package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.FavoritesProfilePreview
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.admin.feature.profile.ui.PublicProfileHeader
import com.coffeepeek.admin.feature.profile.ui.PublicProfileStatsCard
import com.coffeepeek.admin.feature.profile.ui.PublicProfileViewModel
import com.coffeepeek.admin.feature.profile.ui.UserProfileScreen
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun ProfileScreen(vm: ProfileViewModel = koinInject()) {
    val state by vm.uiState.collectAsState()
    LaunchedEffect(vm) {
        if (vm.uiState.value.isLoggedIn && vm.uiState.value.hasContent) vm.refreshProfile()
    }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val onEdit = { Navigator.navigate(Navigator.Screen.EditProfile) }
    val openShop: (String) -> Unit = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }
    val openRoaster: (String) -> Unit = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) }
    val openFavorites = {
        Analytics.track(AnalyticsEvent.FAVORITES_OPENED, "entry_point" to "profile")
        Navigator.navigate(Navigator.Screen.Favorites)
    }
    val actions: @Composable () -> Unit = {
        ProfileActions(onContributions = ::openContributions, onLogout = { showLogoutDialog = true },
            onBlockedAuthors = { Navigator.navigate(Navigator.Screen.BlockedAuthors) })
    }
    val bottomPadding = 20.dp + LocalFloatingNavClearance.current

    if (showLogoutDialog) ProfileLogoutConfirmation(
        onConfirm = { showLogoutDialog = false; vm.logout() },
        onDismiss = { showLogoutDialog = false },
    )

    val slug = state.publicSlug
    if (state.isLoggedIn && slug != null) {
        key(slug) {
            val publicVm: PublicProfileViewModel = platformViewModel(
                key = slug,
                parameters = { parametersOf(slug) },
            )
            val profileDetails = listOf(state.publicAddress?.revision, state.displayName, state.about, state.avatarUrl,
                state.checkInCount, state.addedShopsCount, state.addedRoastersCount, state.followersCount)
            var loadedProfileDetails by rememberSaveable { mutableStateOf(profileDetails) }
            LaunchedEffect(publicVm, profileDetails) {
                if (loadedProfileDetails != profileDetails) {
                    loadedProfileDetails = profileDetails
                    publicVm.refresh()
                }
            }
            UserProfileScreen(
                slug = slug,
                nickname = state.displayName,
                avatarUrl = state.avatarUrl,
                isOwnProfile = true,
                onEditProfile = onEdit,
                onCoffeeShopClick = openShop,
                onRoasterClick = openRoaster,
                onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
                onUserClick = { userSlug, nickname, avatar ->
                    Navigator.navigate(Navigator.Screen.UserProfile(userSlug, nickname, avatar))
                },
                onFavoritesClick = openFavorites,
                onCheckInsClick = { Navigator.navigate(Navigator.Screen.VisitedPlaces) },
                topBarActions = actions,
                favoritesPreview = { FavoritesProfilePreview(openShop, openRoaster) },
                activityCalendar = { PersonalProfileActivityCalendar() },
                bottomContentPadding = bottomPadding,
                vm = publicVm,
            )
        }
        return
    }

    // Cached accounts without a public address keep access to their personal content.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("Профиль", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (state.isLoggedIn) Box(Modifier.align(Alignment.CenterEnd)) { actions() }
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                state.error != null && !state.hasContent -> Box(
                    Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center,
                ) { LoadErrorContent(onRetry = vm::refreshProfile) }
                else -> Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.isLoggedIn) {
                        PublicProfileHeader(
                            nickname = state.displayName,
                            avatarUrl = state.avatarUrl,
                            about = state.about,
                            onEditProfile = onEdit,
                            onAvatarClick = onEdit,
                        )
                        PublicProfileStatsCard(PublicProfileStats(
                            checkIns = state.checkInCount,
                            coffeeShops = state.addedShopsCount,
                            roasters = state.addedRoastersCount,
                            followers = state.followersCount,
                        ))
                        if (state.error != null || state.refreshError != null) LoadErrorContent(onRetry = vm::refreshProfile)
                    } else GuestAuthCard(
                        onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
                        onRegister = { Navigator.navigate(Navigator.Screen.Register) },
                    )
                    TextButton(onClick = openFavorites) { Text("Избранное") }
                    if (state.isLoggedIn) FavoritesProfilePreview(openShop, openRoaster)
                    TextButton(onClick = {
                        Navigator.navigate(if (state.isLoggedIn) Navigator.Screen.VisitedPlaces else Navigator.Screen.Auth)
                    }) { Text("Чекины") }
                    Spacer(Modifier.height(bottomPadding))
                }
            }
        }
    }
}

private fun openContributions(kind: ContributionKind) =
    Navigator.navigate(Navigator.Screen.MyContributions(kind.name))
