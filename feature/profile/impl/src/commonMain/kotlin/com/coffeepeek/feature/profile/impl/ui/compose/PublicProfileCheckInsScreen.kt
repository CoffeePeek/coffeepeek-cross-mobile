package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.compose.component.InlineProfileError
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionLoader
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState

@Composable
internal fun PublicProfileCheckInsScreenContent(
    state: PublicProfileState, slots: ProfileSlots, onBack: () -> Unit, onOpenShop: (String) -> Unit,
    onRetry: () -> Unit, onLoadMore: () -> Unit,
    onPhotoClick: (List<String>, List<String>, Int) -> Unit,
) {
    Scaffold(
        topBar = { CoffeePeekTopBar(stringResource(Res.string.profile_section_check_ins), stringResource(Res.string.profile_back), onBack = onBack) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            when {
                state.profile?.isBlockedByViewer == true -> Text(stringResource(Res.string.profile_blocked_title), Modifier.padding(16.dp))
                state.profile == null && state.isLoading -> ProfileSectionLoader(160.dp)
                state.profile == null -> InlineProfileError(stringResource(Res.string.profile_load_failed), onRetry)
                else -> PublicProfileCheckInsList(state, slots, onOpenShop, onRetry, onLoadMore, onPhotoClick)
            }
        }
    }
}

@Composable
internal fun PublicProfileCheckInsList(
    state: PublicProfileState, slots: ProfileSlots, onOpenShop: (String) -> Unit,
    onRetry: () -> Unit, onLoadMore: () -> Unit, onPhotoClick: (List<String>, List<String>, Int) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    isOwnProfile: Boolean = state.isOwnProfile,
) {
    val profile = state.profile ?: return
    val author = ProfileAuthor(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)
    val listState = rememberLazyListState()
    slots.photoPrefetch(listState, state.checkIns.map { it.id to it.photoPreviewUrls.take(3) })
    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.checkIns, key = { it.id }) { checkIn ->
            slots.checkInCard(checkIn, author, isOwnProfile,
                checkIn.coffeeShopSlug?.takeIf(String::isNotBlank)?.let { slug -> { onOpenShop(slug) } }, onPhotoClick)
        }
        if (state.isLoadingCheckIns || state.isLoadingMoreCheckIns) item { ProfileSectionLoader(88.dp) }
        else if (state.checkInsError != null) item { InlineProfileError(stringResource(Res.string.profile_check_ins_failed), onRetry) }
        else if (state.checkIns.isEmpty()) item { Text(stringResource(Res.string.profile_check_ins_empty)) }
        if (state.hasMoreCheckIns && !state.isLoadingMoreCheckIns && state.checkInsError == null) item {
            TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.profile_load_more)) }
        }
    }
}

@Composable
private fun PublicProfileCheckInsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val slots = ProfileSlots(image = { _, _, _ -> }, photoViewer = { _, _, _, _ -> },
        favoritesPreview = { _, _ -> }, favoriteCard = { _, _, _ -> }, checkInCard = { _, _, _, _, _ -> },
        photoPrefetch = { _, _ -> }, activityPreview = { _, _, _, _, _, _, _ -> })
    PublicProfileCheckInsScreenContent(PublicProfileState(isLoading = false), slots, {}, {}, {}, {}, { _, _, _ -> })
}

@Preview @Composable private fun PublicProfileCheckInsLightPreview() = PublicProfileCheckInsPreview(false)
@Preview @Composable private fun PublicProfileCheckInsDarkPreview() = PublicProfileCheckInsPreview(true)
