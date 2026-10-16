package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.feature.favorites.api.FavoritesProfilePreview
import com.coffeepeek.admin.feature.profile.domain.PublicProfileStats
import com.coffeepeek.admin.feature.profile.ui.PublicProfileHeader
import com.coffeepeek.admin.feature.profile.ui.PublicProfileStatsCard
import com.coffeepeek.admin.feature.profile.ui.PublicProfileViewModel
import com.coffeepeek.admin.feature.profile.ui.UserProfileScreen
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import kotlinx.coroutines.flow.drop
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun ProfileScreen(vm: ProfileViewModel = koinInject()) {
    val state by vm.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val onEdit = { Navigator.navigate(Navigator.Screen.EditProfile) }
    val openShop: (String) -> Unit = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }
    val openRoaster: (String) -> Unit = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) }
    val openFavorites = {
        Analytics.track(AnalyticsEvent.FAVORITES_OPENED, "entry_point" to "profile")
        Navigator.navigate(Navigator.Screen.Favorites)
    }
    val actions: @Composable () -> Unit = {
        ProfileActions(onContributions = ::openContributions, onLogout = { showLogoutDialog = true })
    }
    val bottomPadding = 20.dp + LocalFloatingNavClearance.current

    if (showLogoutDialog) PlatformAlert(
        title = "Выйти из аккаунта?",
        message = "Вы уверены? Потребуется повторный вход.",
        confirmLabel = "Выйти",
        cancelLabel = "Отмена",
        onConfirm = { showLogoutDialog = false; vm.logout() },
        onDismiss = { showLogoutDialog = false },
        destructive = true,
    )

    val slug = state.publicSlug
    if (state.isLoggedIn && slug != null) {
        key(slug) {
            val publicVm: PublicProfileViewModel = koinInject(
                parameters = { parametersOf(slug) },
            )
            DisposableEffect(publicVm) { onDispose(publicVm::close) }
            LaunchedEffect(publicVm) {
                snapshotFlow { state.publicAddress to Triple(state.displayName, state.about, state.avatarUrl) }
                    .drop(1)
                    .collect { publicVm.refresh() }
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
                contentAlignment = Alignment.Center,
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

@Composable
internal fun ProfileActions(onContributions: (ContributionKind) -> Unit, onLogout: () -> Unit) {
    PlatformMenuButton(
        items = listOf(
            PlatformMenuItem("shop-changes", "Правки кофеен", icon = PlatformMenuIcon.NoteEdit, onClick = { onContributions(ContributionKind.Changes) }),
            PlatformMenuItem("submitted-shops", "Отправленные кофейни", icon = PlatformMenuIcon.Add, onClick = { onContributions(ContributionKind.Shops) }),
            PlatformMenuItem("submitted-roasters", "Отправленные обжарщики", icon = PlatformMenuIcon.CoffeeBean, onClick = { onContributions(ContributionKind.Roasters) }),
            PlatformMenuItem("logout", "Выйти", icon = PlatformMenuIcon.Logout, onClick = onLogout),
        ),
        contentDescription = "Действия с профилем",
    ) { openMenu ->
        IconButton(onClick = openMenu) {
            Icon(CpIcons.MoreHorizontal, contentDescription = "Действия с профилем")
        }
    }
}

private fun openContributions(kind: ContributionKind) =
    Navigator.navigate(Navigator.Screen.MyContributions(kind.name))
