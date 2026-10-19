package com.coffeepeek.admin.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.FavoritesPreviewCarousel
import com.coffeepeek.admin.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.admin.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.admin.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.PrefetchListPhotos
import org.koin.core.parameter.parametersOf

private enum class ProfileSection(val title: String) {
    Favorites("Избранное"), CheckIns("Чекины"), Activity("Активность"),
}

private data class ProfilePhotos(val images: PhotoViewerImages, val index: Int = 0)

@Composable
internal fun OtherUserProfileScreen(
    slug: String,
    nickname: String? = null,
    avatarUrl: String? = null,
    onBack: () -> Unit,
    onCoffeeShopClick: (String) -> Unit,
    onRoasterClick: (String) -> Unit,
    onLogin: () -> Unit,
    onUserClick: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
    vm: PublicProfileViewModel = platformViewModel(key = slug, parameters = { parametersOf(slug) }),
) = UserProfileScreen(
    slug = slug,
    nickname = nickname,
    avatarUrl = avatarUrl,
    onBack = onBack,
    onCoffeeShopClick = onCoffeeShopClick,
    onRoasterClick = onRoasterClick,
    onLogin = onLogin,
    onUserClick = onUserClick,
    vm = vm,
)

@Composable
internal fun UserProfileScreen(
    slug: String,
    nickname: String? = null,
    avatarUrl: String? = null,
    onBack: (() -> Unit)? = null,
    onCoffeeShopClick: (String) -> Unit,
    onRoasterClick: (String) -> Unit,
    onLogin: () -> Unit,
    onUserClick: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
    isOwnProfile: Boolean = false,
    onEditProfile: (() -> Unit)? = null,
    onFavoritesClick: (() -> Unit)? = null,
    onCheckInsClick: (() -> Unit)? = null,
    topBarActions: (@Composable () -> Unit)? = null,
    favoritesPreview: (@Composable () -> Unit)? = null,
    activityCalendar: (@Composable () -> Unit)? = null,
    bottomContentPadding: Dp = 20.dp,
    vm: PublicProfileViewModel = platformViewModel(key = slug, parameters = { parametersOf(slug) }),
) {
    val state by vm.state.collectAsState()
    var section by remember(slug) { mutableStateOf<ProfileSection?>(null) }
    var photos by remember(slug) { mutableStateOf<ProfilePhotos?>(null) }
    val checkInsListState = rememberLazyListState()
    val blocks = org.koin.compose.koinInject<com.coffeepeek.feature.authorblocking.api.AuthorBlockActions>()
    val authorVisibility by vm.authorVisibility.collectAsState()
    LaunchedEffect(authorVisibility.revision, state.profile?.slug) { photos = null; section = null }
    val profile = state.profile
    val viewingOwnProfile = isOwnProfile || state.isOwnProfile
    LaunchedEffect(state.needsLogin) {
        if (state.needsLogin) {
            vm.loginHandled()
            onLogin()
        }
    }
    val openFavorite: (PublicProfileFavorite) -> Unit = { favorite ->
        favorite.slug?.takeIf(String::isNotBlank)?.let { target ->
            when (favorite.kind) {
                PublicFavoriteKind.CoffeeShop -> { section = null; onCoffeeShopClick(target) }
                PublicFavoriteKind.Roaster -> { section = null; onRoasterClick(target) }
                PublicFavoriteKind.Drink -> Unit
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(
                    CpColor.GoldWarm.copy(alpha = 0.055f),
                    MaterialTheme.colorScheme.background,
                )))
                .statusBarsPadding(),
        ) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (onBack != null) PlatformGlassIconButton(
                    icon = GlassControlIcon.Back,
                    onClick = onBack,
                    contentDescription = "Назад",
                    modifier = Modifier.align(Alignment.CenterStart),
                ) { Icon(CpIcons.ChevronLeft, contentDescription = null, modifier = Modifier.size(23.dp)) }
                Text(
                    "Профиль", Modifier.fillMaxWidth().padding(horizontal = 56.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                if (!viewingOwnProfile && profile != null) {
                    val author = com.coffeepeek.feature.authorblocking.api.AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)
                    val items = buildList {
                        if (!profile.isBlockedByViewer) profile.canonicalPath?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { path ->
                            add(com.coffeepeek.admin.ui.component.PlatformMenuItem("share", "Поделиться профилем", onClick = { ShareHelper.shareText("https://coffeepeek.by$path") }))
                        }
                        add(com.coffeepeek.admin.ui.component.PlatformMenuItem("block", if (profile.isBlockedByViewer) "Разблокировать автора" else "Заблокировать автора",
                            icon = com.coffeepeek.admin.ui.component.PlatformMenuIcon.Block, destructive = !profile.isBlockedByViewer,
                            onClick = { if (profile.isBlockedByViewer) blocks.requestUnblock(author) else blocks.requestBlock(author) }))
                    }
                    com.coffeepeek.admin.ui.component.PlatformMenuButton(items, "Действия с автором", Modifier.align(Alignment.CenterEnd)) { open ->
                        IconButton(onClick = open) { Icon(CpIcons.MoreHorizontal, "Действия с автором") }
                    }
                } else if (topBarActions != null) Box(Modifier.align(Alignment.CenterEnd)) {
                    topBarActions()
                } else profile?.canonicalPath?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { path ->
                    PlatformGlassIconButton(
                        icon = GlassControlIcon.Share,
                        onClick = { ShareHelper.shareText("https://coffeepeek.by$path") },
                        contentDescription = "Поделиться профилем",
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) { Icon(CpIcons.Share, contentDescription = null, modifier = Modifier.size(21.dp)) }
                }
            }

            if (profile == null) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.isLoading) {
                        PublicProfileHeader(
                            nickname = nickname?.takeIf(String::isNotBlank) ?: slug,
                            avatarUrl = avatarUrl.takeUnless { authorVisibility.hides(null, slug) },
                            about = null,
                            onAvatarClick = { if (!authorVisibility.hides(null, slug)) avatarUrl?.let { photos = ProfilePhotos(PhotoViewerImages(listOf(it))) } },
                        )
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                    } else Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(CpIcons.User, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text(
                            state.error ?: "Не удалось загрузить профиль",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = vm::refresh) { Text("Попробовать снова") }
                    }
                }
            } else if (profile.isBlockedByViewer) {
                com.coffeepeek.admin.ui.component.CpEmptyState(
                    title = "Автор заблокирован",
                    message = "Публикации автора «${profile.nickname}» скрыты. Вы можете снять блокировку в любой момент.",
                    icon = CpIcons.VisibilityOff,
                    modifier = Modifier.padding(top = com.coffeepeek.admin.theme.CpDimens.spacing6),
                    actionLabel = "Разблокировать",
                    onAction = { blocks.requestUnblock(com.coffeepeek.feature.authorblocking.api.AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)) },
                )
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(2.dp))
                    PublicProfileHeader(
                        profile.nickname, profile.avatarUrl, profile.about,
                        isFollowing = state.isFollowing,
                        followEnabled = !state.isFollowLoading && !state.isLoading,
                        onFollow = if (viewingOwnProfile) null else vm::toggleFollow,
                        onEditProfile = onEditProfile,
                    ) {
                        profile.avatarUrl?.let { photos = ProfilePhotos(PhotoViewerImages(listOf(it))) }
                    }
                    PublicProfileStatsCard(profile.stats, onFollowersClick = {
                        section = null
                        vm.openConnections(PublicProfileConnectionKind.Followers)
                    })
                    if (state.error != null) InlineProfileError("Не удалось обновить профиль", vm::refresh)
                    if (!viewingOwnProfile && state.followError != null) InlineProfileError("Не удалось изменить подписку", vm::toggleFollow)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileSectionHeading("Избранное", onAll = onFavoritesClick ?: { section = ProfileSection.Favorites })
                        if (favoritesPreview != null) favoritesPreview() else when {
                            state.isLoadingFavorites && profile.favorites.isEmpty() -> ProfileSectionLoader()
                            state.favoritesError != null && profile.favorites.isEmpty() ->
                                InlineProfileError("Не удалось загрузить избранное", vm::retryFavorites)
                            profile.favorites.isEmpty() -> ProfileEmptyCard("Пока ничего нет в избранном", CpIcons.Favorite)
                            else -> {
                                val favorites = profile.favorites.take(6)
                                FavoritesPreviewCarousel(
                                    coffeeShops = favorites.filter { it.kind == PublicFavoriteKind.CoffeeShop },
                                    roasters = favorites.filter { it.kind == PublicFavoriteKind.Roaster },
                                    drinks = favorites.filter { it.kind == PublicFavoriteKind.Drink },
                                    itemKey = { it.id },
                                ) { favorite, modifier ->
                                    PublicProfileFavoriteCard(
                                        favorite,
                                        modifier = modifier,
                                        onClick = { openFavorite(favorite) },
                                    )
                                }
                                if (state.favoritesError != null) InlineProfileError("Не удалось обновить избранное", vm::retryFavorites)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileSectionHeading("Чекины", onAll = onCheckInsClick ?: { section = ProfileSection.CheckIns })
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when {
                                state.isLoadingCheckIns -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    CoffeePeekLoader()
                                }
                                state.checkInsError != null && state.checkIns.isEmpty() ->
                                    InlineProfileError("Не удалось загрузить чекины", vm::retryCheckIns)
                                state.checkIns.isEmpty() -> ProfileEmptyCard("Пока нет чекинов", CpIcons.Coffee)
                                else -> state.checkIns.take(2).forEach { checkIn ->
                                    PublicProfileCheckInCard(
                                        checkIn = checkIn,
                                        onShopClick = { checkIn.coffeeShopSlug?.let(onCoffeeShopClick) },
                                        onPhotoClick = { urls, index -> photos = ProfilePhotos(urls, index) },
                                        authorName = profile.nickname,
                                        authorAvatarUrl = profile.avatarUrl,
                                        isOwnProfile = viewingOwnProfile,
                                        author = com.coffeepeek.feature.authorblocking.api.AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl),
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileSectionHeading("Активность", onAll = { section = ProfileSection.Activity })
                        when {
                            state.isLoadingActivity -> ProfileSectionLoader()
                            state.activityError != null -> InlineProfileError("Не удалось загрузить активность", vm::retryActivity)
                            profile.activity != null -> PublicProfileActivityCard(profile.activity, onClick = { section = ProfileSection.Activity })
                            else -> ProfileSectionLoader()
                        }
                    }
                    Spacer(Modifier.height(bottomContentPadding))
                }
            }
        }
    }

    // Native sheet content can recompose while its controller is being dismissed.
    val selectedSection = section
    if (selectedSection == ProfileSection.CheckIns && profile != null && !profile.isBlockedByViewer) {
        PrefetchListPhotos(checkInsListState, state.checkIns.map { checkIn ->
            checkIn.id to (checkIn.source?.photoThumbnailUrls ?: checkIn.photoUrls).take(3)
        })
    }
    if (selectedSection != null && profile != null && !profile.isBlockedByViewer) SwipeDismissModalBottomSheet(
        onDismissRequest = { section = null },
        maxSheetHeightFraction = 0.9f,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(selectedSection.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { section = null }) { Icon(CpIcons.Close, contentDescription = "Закрыть") }
        }
        when (selectedSection) {
            ProfileSection.Favorites -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(profile.favorites, key = { "${it.kind}-${it.id}" }) { favorite ->
                    PublicProfileFavoriteCard(favorite, modifier = Modifier.fillMaxWidth(), onClick = { openFavorite(favorite) })
                }
                if (state.isLoadingFavorites) item { ProfileSectionLoader() }
                else if (state.favoritesError != null) item { InlineProfileError("Не удалось загрузить избранное", vm::retryFavorites) }
                else if (profile.favorites.isEmpty()) item { ProfileEmptyCard("Пока ничего нет в избранном", CpIcons.Favorite) }
            }
            ProfileSection.CheckIns -> LazyColumn(
                state = checkInsListState,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.checkIns, key = { it.id }) { checkIn ->
                    PublicProfileCheckInCard(
                        checkIn = checkIn,
                        onShopClick = { checkIn.coffeeShopSlug?.let { section = null; onCoffeeShopClick(it) } },
                        onPhotoClick = { urls, index -> photos = ProfilePhotos(urls, index) },
                        authorName = profile.nickname,
                        authorAvatarUrl = profile.avatarUrl,
                        isOwnProfile = viewingOwnProfile,
                        author = com.coffeepeek.feature.authorblocking.api.AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl),
                    )
                }
                if (state.isLoadingCheckIns || state.isLoadingMoreCheckIns) item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                } else if (state.checkInsError != null) item {
                    InlineProfileError("Не удалось загрузить чекины", vm::retryCheckIns)
                } else if (state.checkIns.isEmpty()) item { ProfileEmptyCard("Пока нет чекинов", CpIcons.Coffee) }
                if (state.hasMoreCheckIns && !state.isLoadingMoreCheckIns && state.checkInsError == null) item {
                    TextButton(onClick = vm::loadMoreCheckIns, modifier = Modifier.fillMaxWidth()) { Text("Загрузить ещё") }
                }
            }
            ProfileSection.Activity -> Column(
                Modifier.fillMaxWidth().weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            ) {
                if (viewingOwnProfile && activityCalendar != null) activityCalendar() else PublicProfileActivityCalendar(
                    activity = profile.activity,
                    selectedMonth = state.activityMonth,
                    isLoading = state.isLoadingActivity,
                    error = state.activityError,
                    onMonthChange = vm::changeActivityMonth,
                    onRetry = vm::retryActivity,
                )
            }
        }
    }
    if (state.connectionKind != null) PublicProfileConnectionsSheet(
        state = state,
        onDismiss = vm::closeConnections,
        onSelectKind = vm::openConnections,
        onLoadMore = vm::loadMoreConnections,
        onRetry = vm::retryConnections,
        onUserClick = { user ->
            vm.closeConnections()
            onUserClick(user.slug, user.userName, user.avatarUrl)
        },
    )
    photos?.let { FullScreenImageDialog(images = it.images, initialIndex = it.index, onDismiss = { photos = null }) }
}

@Composable
private fun ProfileSectionLoader() {
    Box(Modifier.fillMaxWidth().height(88.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
}

@Composable
private fun ProfileSectionHeading(title: String, onAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(
            onClick = onAll,
            modifier = Modifier.semantics { contentDescription = "Все: $title" },
            contentPadding = PaddingValues(start = 12.dp),
        ) {
            Text("Все", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(3.dp))
            Icon(CpIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun InlineProfileError(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text("Повторить") }
    }
}
