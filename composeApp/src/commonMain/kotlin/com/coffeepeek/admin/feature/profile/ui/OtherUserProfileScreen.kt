package com.coffeepeek.admin.feature.profile.ui

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.FavoritesPreviewCarousel
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.ShareHelper
import org.koin.core.parameter.parametersOf

private enum class ProfileSection(val title: org.jetbrains.compose.resources.StringResource) {
    Favorites(Res.string.favorites_profile_title), CheckIns(Res.string.profile_stat_check_ins), Activity(Res.string.legacy_profile_section_profile_section_activity),
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
    onCheckInsClick: (() -> Unit)? = null,
    checkInsOnly: Boolean = false,
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
    onCheckInsClick = onCheckInsClick,
    checkInsOnly = checkInsOnly,
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
    favoritesPreview: (@Composable (header: @Composable () -> Unit) -> Unit)? = null,
    ownCheckInCount: Int = 0,
    activityCalendar: (@Composable () -> Unit)? = null,
    activityPreview: (@Composable (onClick: () -> Unit) -> Unit)? = null,
    bottomContentPadding: Dp = 20.dp,
    checkInsOnly: Boolean = false,
    vm: PublicProfileViewModel = platformViewModel(key = slug, parameters = { parametersOf(slug) }),
) {
    val localizedProfileCheckinsBack = stringResource(Res.string.profile_checkins_back)
    val localizedLegacyMainScreenProfileTitle = stringResource(Res.string.legacy_main_screen_profile_title)
    val localizedLegacyOtherUserProfileScreenProfileShare = stringResource(Res.string.legacy_other_user_profile_screen_profile_share)
    val localizedLegacyOtherUserProfileScreenProfileUnblockAuthor = stringResource(Res.string.legacy_other_user_profile_screen_profile_unblock_author)
    val localizedLegacyCheckInActionsCommunityBlockAuthor = stringResource(Res.string.legacy_check_in_actions_community_block_author)
    val localizedLegacyOtherUserProfileScreenProfileAuthorActions = stringResource(Res.string.legacy_other_user_profile_screen_profile_author_actions)
    val localizedLegacyOtherUserProfileScreenProfileLoadFailed = stringResource(Res.string.legacy_other_user_profile_screen_profile_load_failed)
    val localizedRetry = stringResource(Res.string.retry)
    val localizedProfileCheckinsBlocked = stringResource(Res.string.profile_checkins_blocked)
    val localizedLegacyAuthorBlockingHostProfileUnblock = stringResource(Res.string.legacy_author_blocking_host_profile_unblock)
    val localizedLegacyOtherUserProfileScreenProfileUpdateFailed = stringResource(Res.string.legacy_other_user_profile_screen_profile_update_failed)
    val localizedLegacyOtherUserProfileScreenProfileFollowFailed = stringResource(Res.string.legacy_other_user_profile_screen_profile_follow_failed)
    val localizedFavoritesProfileTitle = stringResource(Res.string.favorites_profile_title)
    val localizedLegacyOtherUserProfileScreenRoasterMessageFavoritesUnavailable = stringResource(Res.string.legacy_other_user_profile_screen_roaster_message_favorites_unavailable)
    val localizedLegacyOtherUserProfileScreenProfileFavoritesRefreshFailed = stringResource(Res.string.legacy_other_user_profile_screen_profile_favorites_refresh_failed)
    val localizedProfileStatCheckIns = stringResource(Res.string.profile_stat_check_ins)
    val localizedProfileCheckinsError = stringResource(Res.string.profile_checkins_error)
    val localizedHistoryHomeClose = stringResource(Res.string.history_home_close)

    val state by vm.state.collectAsState()
    var section by remember(slug) { mutableStateOf<ProfileSection?>(null) }
    var photos by remember(slug) { mutableStateOf<ProfilePhotos?>(null) }
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

    if (checkInsOnly) {
        PublicProfileCheckInsScreenContent(state, onBack ?: {}, onCoffeeShopClick,
            onRetry = if (profile == null) vm::refresh else vm::retryCheckIns,
            onLoadMore = vm::loadMoreCheckIns,
            onPhotoClick = { images, index -> photos = ProfilePhotos(images, index) })
        photos?.let { FullScreenImageDialog(images = it.images, initialIndex = it.index, onDismiss = { photos = null }) }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding(),
        ) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (onBack != null) PlatformGlassIconButton(
                    icon = GlassControlIcon.Back,
                    onClick = onBack,
                    contentDescription = localizedProfileCheckinsBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) { Icon(CpIcons.ChevronLeft, contentDescription = null, modifier = Modifier.size(23.dp)) }
                Text(
                    localizedLegacyMainScreenProfileTitle, Modifier.fillMaxWidth().padding(start = if (onBack == null) 0.dp else 56.dp, end = 56.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = if (onBack == null) TextAlign.Start else TextAlign.Center,
                )
                if (!viewingOwnProfile && profile != null) {
                    val author = com.coffeepeek.feature.authorblocking.api.AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)
                    val items = buildList {
                        if (!profile.isBlockedByViewer) profile.canonicalPath?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { path ->
                            add(com.coffeepeek.admin.ui.component.PlatformMenuItem("share", localizedLegacyOtherUserProfileScreenProfileShare, onClick = { ShareHelper.shareText("https://coffeepeek.by$path") }))
                        }
                        add(com.coffeepeek.admin.ui.component.PlatformMenuItem("block", if (profile.isBlockedByViewer) localizedLegacyOtherUserProfileScreenProfileUnblockAuthor else localizedLegacyCheckInActionsCommunityBlockAuthor,
                            icon = com.coffeepeek.admin.ui.component.PlatformMenuIcon.Block, destructive = !profile.isBlockedByViewer,
                            onClick = { if (profile.isBlockedByViewer) blocks.requestUnblock(author) else blocks.requestBlock(author) }))
                    }
                    com.coffeepeek.admin.ui.component.PlatformMenuButton(items, localizedLegacyOtherUserProfileScreenProfileAuthorActions, Modifier.align(Alignment.CenterEnd)) { open ->
                        IconButton(onClick = open) { Icon(CpIcons.MoreHorizontal, localizedLegacyOtherUserProfileScreenProfileAuthorActions) }
                    }
                } else if (topBarActions != null) Box(Modifier.align(Alignment.CenterEnd)) {
                    topBarActions()
                } else profile?.canonicalPath?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { path ->
                    PlatformGlassIconButton(
                        icon = GlassControlIcon.Share,
                        onClick = { ShareHelper.shareText("https://coffeepeek.by$path") },
                        contentDescription = localizedLegacyOtherUserProfileScreenProfileShare,
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
                            state.error ?: localizedLegacyOtherUserProfileScreenProfileLoadFailed,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = vm::refresh) { Text(localizedRetry) }
                    }
                }
            } else if (profile.isBlockedByViewer) {
                com.coffeepeek.admin.ui.component.CpEmptyState(
                    title = localizedProfileCheckinsBlocked,
                    message = stringResource(Res.string.legacy_profile_blocked_publications, profile.nickname),
                    icon = CpIcons.VisibilityOff,
                    modifier = Modifier.padding(top = com.coffeepeek.admin.theme.CpDimens.spacing6),
                    actionLabel = localizedLegacyAuthorBlockingHostProfileUnblock,
                    onAction = { blocks.requestUnblock(com.coffeepeek.feature.authorblocking.api.AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)) },
                )
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(2.dp))
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PublicProfileHeader(
                            profile.nickname, profile.avatarUrl, profile.about,
                            isFollowing = state.isFollowing,
                            followEnabled = !state.isFollowLoading && !state.isLoading,
                            onFollow = if (viewingOwnProfile) null else vm::toggleFollow,
                            onEditProfile = onEditProfile,
                            stats = profile.stats,
                            onFollowersClick = {
                                section = null
                                vm.openConnections(PublicProfileConnectionKind.Followers)
                            },
                        ) {
                            profile.avatarUrl?.let { photos = ProfilePhotos(PhotoViewerImages(listOf(it))) }
                        }
                        if (activityPreview != null) activityPreview(onCheckInsClick ?: { section = ProfileSection.CheckIns })
                        else PublicProfileActivityPreview(slug, profile.activity,
                            state.checkIns.map { it.occurredAt to (it.source?.photoThumbnailUrls ?: it.photoUrls) },
                            state.isLoadingActivity || state.isLoadingCheckIns,
                            state.activityError != null || state.checkInsError != null,
                            {
                                vm.retryActivity()
                                vm.retryCheckIns()
                            }, onCheckInsClick ?: { section = ProfileSection.CheckIns })
                        if (state.error != null) InlineProfileError(localizedLegacyOtherUserProfileScreenProfileUpdateFailed, vm::refresh)
                        if (!viewingOwnProfile && state.followError != null) InlineProfileError(localizedLegacyOtherUserProfileScreenProfileFollowFailed, vm::toggleFollow)
                    }
                    if (favoritesPreview != null) favoritesPreview {
                        ProfileSectionHeading(localizedFavoritesProfileTitle, onAll = onFavoritesClick ?: { section = ProfileSection.Favorites })
                    } else if (profile.favorites.isNotEmpty() || state.isLoadingFavorites || state.favoritesError != null) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            ProfileSectionHeading(localizedFavoritesProfileTitle, onAll = onFavoritesClick ?: { section = ProfileSection.Favorites })
                        }
                        when {
                            state.isLoadingFavorites && profile.favorites.isEmpty() -> ProfileSectionLoader()
                            state.favoritesError != null && profile.favorites.isEmpty() ->
                                Box(Modifier.padding(horizontal = 16.dp)) {
                                    InlineProfileError(localizedLegacyOtherUserProfileScreenRoasterMessageFavoritesUnavailable, vm::retryFavorites)
                                }
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
                                if (state.favoritesError != null) Box(Modifier.padding(horizontal = 16.dp)) {
                                    InlineProfileError(localizedLegacyOtherUserProfileScreenProfileFavoritesRefreshFailed, vm::retryFavorites)
                                }
                            }
                        }
                    }

                    if (state.checkIns.isNotEmpty() || state.isLoadingCheckIns || state.checkInsError != null || ownCheckInCount > 0) Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileSectionHeading(localizedProfileStatCheckIns, onAll = onCheckInsClick ?: { section = ProfileSection.CheckIns })
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when {
                                state.isLoadingCheckIns && state.checkIns.isEmpty() -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    CoffeePeekLoader()
                                }
                                state.checkInsError != null && state.checkIns.isEmpty() ->
                                    InlineProfileError(localizedProfileCheckinsError, vm::retryCheckIns)
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

                    Spacer(Modifier.height(bottomContentPadding))
                }
            }
        }
    }

    // Native sheet content can recompose while its controller is being dismissed.
    val selectedSection = section
    if (selectedSection != null && profile != null && !profile.isBlockedByViewer) SwipeDismissModalBottomSheet(
        onDismissRequest = { section = null },
        maxSheetHeightFraction = 0.9f,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(selectedSection.title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { section = null }) { Icon(CpIcons.Close, contentDescription = localizedHistoryHomeClose) }
        }
        when (selectedSection) {
            ProfileSection.Favorites -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = coffeePeekScrollContentPadding(PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(profile.favorites, key = { "${it.kind}-${it.id}" }) { favorite ->
                    PublicProfileFavoriteCard(favorite, modifier = Modifier.fillMaxWidth(), onClick = { openFavorite(favorite) })
                }
                if (state.isLoadingFavorites) item { ProfileSectionLoader() }
                else if (state.favoritesError != null) item { InlineProfileError(localizedLegacyOtherUserProfileScreenRoasterMessageFavoritesUnavailable, vm::retryFavorites) }
            }
            ProfileSection.CheckIns -> PublicProfileCheckInsList(
                state, { section = null; onCoffeeShopClick(it) }, vm::retryCheckIns, vm::loadMoreCheckIns,
                onPhotoClick = { images, index -> photos = ProfilePhotos(images, index) },
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                isOwnProfile = viewingOwnProfile,
            )
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
    val description = stringResource(Res.string.legacy_profile_all_section, title)

    val localizedLegacyProfileCheckInsScreenRoasterTagAll = stringResource(Res.string.legacy_profile_check_ins_screen_roaster_tag_all)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(
            onClick = onAll,
            modifier = Modifier.semantics { contentDescription = description },
            contentPadding = PaddingValues(start = 12.dp),
        ) {
            Text(localizedLegacyProfileCheckInsScreenRoasterTagAll, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(3.dp))
            Icon(CpIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun InlineProfileError(text: String, onRetry: () -> Unit) {
    val localizedHistoryHomeRetry = stringResource(Res.string.history_home_retry)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text(localizedHistoryHomeRetry) }
    }
}
