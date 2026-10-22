package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.feature.profile.impl.R
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.compose.component.InlineProfileError
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileEmptyCard
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionHeading
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionLoader
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileActivityCalendar
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileConnectionsSheet
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileHeader
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileEvent

private enum class ProfileSection { Favorites, CheckIns, Activity }

private data class ProfilePhotos(val previews: List<String>, val originals: List<String> = previews, val index: Int = 0)

private const val PUBLIC_PROFILE_BASE_URL = "https://coffeepeek.by"

/** What the owner's own profile changes on the shared screen: no back button, owner actions, owner-only slots and destinations. */
internal class OwnProfileExtras(
    val onEditProfile: () -> Unit,
    val onOpenFavorites: () -> Unit,
    val onOpenCheckIns: () -> Unit,
    val topBarActions: @Composable () -> Unit,
    val favoritesPreview: @Composable () -> Unit,
    val activityCalendar: @Composable () -> Unit,
    val activityPreview: @Composable (onClick: () -> Unit) -> Unit,
    val bottomContentPadding: Dp,
)

@Composable
internal fun PublicProfileScreen(
    slug: String,
    nickname: String?,
    avatarUrl: String?,
    vm: PublicProfileViewModel,
    authorActions: ProfileAuthorActions,
    slots: ProfileSlots,
    onBack: (() -> Unit)?,
    onOpenShop: (String) -> Unit,
    onOpenRoaster: (String) -> Unit,
    onLogin: () -> Unit,
    onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
    onShare: (url: String) -> Unit,
    own: OwnProfileExtras? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val visibility by vm.authorVisibility.collectAsStateWithLifecycle(initialValue = vm.currentVisibility())
    var section by remember(slug) { mutableStateOf<ProfileSection?>(null) }
    var photos by remember(slug) { mutableStateOf<ProfilePhotos?>(null) }
    val checkInsListState = rememberLazyListState()
    val profile = state.profile
    val viewingOwn = own != null || state.isOwnProfile
    LaunchedEffect(visibility.revision, profile?.slug) { photos = null; section = null }
    LaunchedEffect(vm) { vm.events.collect { event -> when (event) { PublicProfileEvent.RequireLogin -> onLogin() } } }
    val sharePath = profile?.canonicalPath?.takeIf { it.startsWith("/") && !it.startsWith("//") }
    val author = profile?.let { ProfileAuthor(it.authorId, it.slug, it.nickname, it.avatarUrl) }
    val openFavorite: (PublicProfileFavorite) -> Unit = { favorite ->
        favorite.slug?.takeIf(String::isNotBlank)?.let { target ->
            when (favorite.kind) {
                PublicFavoriteKind.CoffeeShop -> { section = null; onOpenShop(target) }
                PublicFavoriteKind.Roaster -> { section = null; onOpenRoaster(target) }
                PublicFavoriteKind.Drink -> Unit
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding)
                .statusBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp), contentAlignment = Alignment.Center) {
                if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(CoffeePeekIcons.ChevronLeft, contentDescription = stringResource(R.string.profile_back), modifier = Modifier.size(23.dp))
                }
                Text(
                    stringResource(R.string.profile_title),
                    Modifier.fillMaxWidth().padding(start = if (onBack == null) 0.dp else 56.dp, end = 56.dp),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    textAlign = if (onBack == null) TextAlign.Start else TextAlign.Center,
                )
                if (own != null) Box(Modifier.align(Alignment.CenterEnd)) { own.topBarActions() }
                else if (profile != null && author != null && !viewingOwn) Box(Modifier.align(Alignment.CenterEnd)) {
                    PublicProfileActions(
                        isBlocked = profile.isBlockedByViewer,
                        onShare = sharePath?.let { path -> { onShare("$PUBLIC_PROFILE_BASE_URL$path") } },
                        onBlock = { if (profile.isBlockedByViewer) authorActions.requestUnblock(author) else authorActions.requestBlock(author) },
                    )
                } else if (profile != null && sharePath != null) IconButton(
                    onClick = { onShare("$PUBLIC_PROFILE_BASE_URL$sharePath") }, modifier = Modifier.align(Alignment.CenterEnd),
                ) { Icon(CoffeePeekIcons.Share, stringResource(R.string.profile_share), modifier = Modifier.size(21.dp)) }
            }

            if (profile == null || author == null) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.isLoading) {
                        val hidden = visibility.hides(null, slug)
                        PublicProfileHeader(
                            nickname = nickname?.takeIf(String::isNotBlank) ?: slug,
                            avatarUrl = avatarUrl.takeUnless { hidden },
                            about = null,
                            image = slots.image,
                            onAvatarClick = { if (!hidden) avatarUrl?.let { photos = ProfilePhotos(listOf(it)) } },
                        )
                        ProfileSectionLoader(160.dp)
                    } else Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(CoffeePeekIcons.User, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text(state.error ?: stringResource(R.string.profile_load_failed), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Button(onClick = { vm.onAction(PublicProfileAction.Refresh) }) { Text(stringResource(R.string.profile_retry_full)) }
                    }
                }
            } else if (profile.isBlockedByViewer) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CoffeePeekIcons.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.profile_blocked_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.profile_blocked_message, profile.nickname), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Button(onClick = { authorActions.requestUnblock(author) }) { Text(stringResource(R.string.profile_unblock)) }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                        .then(if (own == null) Modifier.navigationBarsPadding() else Modifier),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(2.dp))
                    PublicProfileHeader(
                        profile.nickname, profile.avatarUrl, profile.about, slots.image,
                        isFollowing = state.isFollowing,
                        followEnabled = !state.isFollowLoading && !state.isLoading,
                        onFollow = if (viewingOwn) null else ({ vm.onAction(PublicProfileAction.ToggleFollow) }),
                        onEditProfile = own?.onEditProfile,
                        stats = profile.stats,
                        onFollowersClick = {
                            section = null
                            vm.onAction(PublicProfileAction.OpenConnections(PublicProfileConnectionKind.Followers))
                        },
                    ) { profile.avatarUrl?.let { photos = ProfilePhotos(listOf(it)) } }
                    if (own != null) own.activityPreview { section = ProfileSection.Activity }
                    else slots.activityPreview(slug, profile.activity, state.checkIns,
                        state.isLoadingActivity || state.isLoadingCheckIns,
                        state.activityError != null || state.checkInsError != null,
                        {
                            vm.onAction(PublicProfileAction.RetryActivity)
                            vm.onAction(PublicProfileAction.RetryCheckIns)
                        },
                        { section = ProfileSection.Activity })
                    if (state.error != null) InlineProfileError(stringResource(R.string.profile_update_failed)) { vm.onAction(PublicProfileAction.Refresh) }
                    if (!viewingOwn && state.followError != null) {
                        InlineProfileError(stringResource(R.string.profile_follow_failed)) { vm.onAction(PublicProfileAction.ToggleFollow) }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileSectionHeading(stringResource(R.string.profile_section_favorites), onAll = own?.onOpenFavorites ?: { section = ProfileSection.Favorites })
                        if (own != null) own.favoritesPreview() else when {
                            state.isLoadingFavorites && profile.favorites.isEmpty() -> ProfileSectionLoader()
                            state.favoritesError != null && profile.favorites.isEmpty() ->
                                InlineProfileError(stringResource(R.string.profile_favorites_failed)) { vm.onAction(PublicProfileAction.RetryFavorites) }
                            profile.favorites.isEmpty() -> ProfileEmptyCard(stringResource(R.string.profile_favorites_empty), CoffeePeekIcons.Favorite)
                            else -> {
                                slots.favoritesPreview(profile.favorites.take(6), openFavorite)
                                if (state.favoritesError != null) {
                                    InlineProfileError(stringResource(R.string.profile_favorites_refresh_failed)) { vm.onAction(PublicProfileAction.RetryFavorites) }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileSectionHeading(stringResource(R.string.profile_section_check_ins), onAll = own?.onOpenCheckIns ?: { section = ProfileSection.CheckIns })
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when {
                                state.isLoadingCheckIns -> ProfileSectionLoader(100.dp)
                                state.checkInsError != null && state.checkIns.isEmpty() ->
                                    InlineProfileError(stringResource(R.string.profile_check_ins_failed)) { vm.onAction(PublicProfileAction.RetryCheckIns) }
                                state.checkIns.isEmpty() -> ProfileEmptyCard(stringResource(R.string.profile_check_ins_empty), CoffeePeekIcons.Coffee)
                                else -> state.checkIns.take(2).forEach { checkIn ->
                                    slots.checkInCard(checkIn, author, viewingOwn,
                                        checkIn.coffeeShopSlug?.takeIf(String::isNotBlank)?.let { shop -> { onOpenShop(shop) } }) { previews, originals, index -> photos = ProfilePhotos(previews, originals, index) }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(own?.bottomContentPadding ?: 20.dp))
                }
            }
        }
    }

    val selectedSection = section
    if (selectedSection == ProfileSection.CheckIns && profile != null && author != null && !profile.isBlockedByViewer) {
        slots.photoPrefetch(checkInsListState, state.checkIns.map { it.id to it.photoPreviewUrls.take(3) })
    }
    if (selectedSection != null && profile != null && author != null && !profile.isBlockedByViewer) {
        val close = stringResource(R.string.profile_close)
        CoffeePeekSwipeDismissModalBottomSheet(onDismissRequest = { section = null }, dismissDescription = close, maxSheetHeightFraction = 0.9f) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(when (selectedSection) {
                        ProfileSection.Favorites -> R.string.profile_section_favorites
                        ProfileSection.CheckIns -> R.string.profile_section_check_ins
                        ProfileSection.Activity -> R.string.profile_section_activity
                    }),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { section = null }) { Icon(CoffeePeekIcons.Close, contentDescription = close) }
            }
            when (selectedSection) {
                ProfileSection.Favorites -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(profile.favorites, key = { "${it.kind}-${it.id}" }) { favorite ->
                        slots.favoriteCard(favorite, Modifier.fillMaxWidth()) { openFavorite(favorite) }
                    }
                    if (state.isLoadingFavorites) item { ProfileSectionLoader() }
                    else if (state.favoritesError != null) item {
                        InlineProfileError(stringResource(R.string.profile_favorites_failed)) { vm.onAction(PublicProfileAction.RetryFavorites) }
                    }
                    else if (profile.favorites.isEmpty()) item { ProfileEmptyCard(stringResource(R.string.profile_favorites_empty), CoffeePeekIcons.Favorite) }
                }
                ProfileSection.CheckIns -> LazyColumn(
                    state = checkInsListState,
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.checkIns, key = { it.id }) { checkIn ->
                        slots.checkInCard(checkIn, author, viewingOwn,
                            checkIn.coffeeShopSlug?.takeIf(String::isNotBlank)?.let { shop -> { section = null; onOpenShop(shop) } }) { previews, originals, index -> photos = ProfilePhotos(previews, originals, index) }
                    }
                    if (state.isLoadingCheckIns || state.isLoadingMoreCheckIns) item { ProfileSectionLoader(88.dp) }
                    else if (state.checkInsError != null) item {
                        InlineProfileError(stringResource(R.string.profile_check_ins_failed)) { vm.onAction(PublicProfileAction.RetryCheckIns) }
                    }
                    else if (state.checkIns.isEmpty()) item { ProfileEmptyCard(stringResource(R.string.profile_check_ins_empty), CoffeePeekIcons.Coffee) }
                    if (state.hasMoreCheckIns && !state.isLoadingMoreCheckIns && state.checkInsError == null) item {
                        TextButton(onClick = { vm.onAction(PublicProfileAction.LoadMoreCheckIns) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.profile_load_more))
                        }
                    }
                }
                ProfileSection.Activity -> Column(
                    Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
                ) {
                    if (viewingOwn && own != null) own.activityCalendar() else PublicProfileActivityCalendar(
                        activity = profile.activity,
                        selectedMonth = state.activityMonth,
                        isLoading = state.isLoadingActivity,
                        error = state.activityError,
                        onMonthChange = { vm.onAction(PublicProfileAction.ChangeActivityMonth(it)) },
                        onRetry = { vm.onAction(PublicProfileAction.RetryActivity) },
                    )
                }
            }
        }
    }
    if (state.connectionKind != null) PublicProfileConnectionsSheet(
        state = state,
        image = slots.image,
        onDismiss = { vm.onAction(PublicProfileAction.CloseConnections) },
        onSelectKind = { vm.onAction(PublicProfileAction.OpenConnections(it)) },
        onLoadMore = { vm.onAction(PublicProfileAction.LoadMoreConnections) },
        onRetry = { vm.onAction(PublicProfileAction.RetryConnections) },
        onUserClick = { user ->
            vm.onAction(PublicProfileAction.CloseConnections)
            onOpenUser(user.slug, user.userName, user.avatarUrl)
        },
    )
    photos?.let { slots.photoViewer(it.previews, it.originals, it.index) { photos = null } }
}
