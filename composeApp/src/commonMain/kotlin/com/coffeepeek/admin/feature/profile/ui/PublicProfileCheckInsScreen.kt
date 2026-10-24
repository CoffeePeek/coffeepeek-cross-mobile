package com.coffeepeek.admin.feature.profile.ui

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.component.coffeePeekViewportBottomPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.utils.PrefetchListPhotos
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PublicProfileCheckInsScreenContent(
    state: PublicProfileUiState, onBack: () -> Unit, onOpenShop: (String) -> Unit,
    onRetry: () -> Unit, onLoadMore: () -> Unit, onPhotoClick: (PhotoViewerImages, Int) -> Unit,
) {
    Scaffold(
        topBar = { CoffeePeekTopBar(stringResource(Res.string.profile_stat_check_ins), stringResource(Res.string.profile_checkins_back), onBack = onBack) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).coffeePeekViewportBottomPadding()) {
            when {
                state.profile?.isBlockedByViewer == true -> Text(stringResource(Res.string.profile_checkins_blocked), Modifier.padding(16.dp))
                state.profile == null && state.isLoading -> CoffeePeekLoader(stringResource(Res.string.profile_week_loading))
                state.profile == null -> PublicCheckInsError(onRetry)
                else -> PublicProfileCheckInsList(state, onOpenShop, onRetry, onLoadMore, onPhotoClick)
            }
        }
    }
}

@Composable
internal fun PublicProfileCheckInsList(
    state: PublicProfileUiState, onOpenShop: (String) -> Unit,
    onRetry: () -> Unit, onLoadMore: () -> Unit, onPhotoClick: (PhotoViewerImages, Int) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    isOwnProfile: Boolean = state.isOwnProfile,
) {
    val profile = state.profile ?: return
    val listState = rememberLazyListState()
    PrefetchListPhotos(listState, state.checkIns.map { it.id to (it.source?.photoThumbnailUrls ?: it.photoUrls).take(3) })
    LazyColumn(state = listState, modifier = modifier, contentPadding = coffeePeekScrollContentPadding(PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.checkIns, key = { it.id }) { checkIn ->
            PublicProfileCheckInCard(checkIn, { checkIn.coffeeShopSlug?.takeIf(String::isNotBlank)?.let(onOpenShop) }, onPhotoClick,
                profile.nickname, profile.avatarUrl, isOwnProfile,
                AuthorReference(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl))
        }
        if (state.isLoadingCheckIns || state.isLoadingMoreCheckIns) item { CoffeePeekLoader(stringResource(Res.string.profile_week_loading)) }
        else if (state.checkInsError != null) item { PublicCheckInsError(onRetry) }
        else if (state.checkIns.isEmpty()) item { Text(stringResource(Res.string.profile_checkins_empty)) }
        if (state.hasMoreCheckIns && !state.isLoadingMoreCheckIns && state.checkInsError == null) item {
            TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.profile_checkins_more)) }
        }
    }
}

@Composable
private fun PublicCheckInsError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(stringResource(Res.string.profile_checkins_error))
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.profile_week_retry)) }
    }
}

@Composable
private fun PublicCheckInsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PublicProfileCheckInsScreenContent(PublicProfileUiState(isLoading = false), {}, {}, {}, {}, { _, _ -> })
}

@Preview @Composable private fun PublicCheckInsLightPreview() = PublicCheckInsPreview(false)
@Preview @Composable private fun PublicCheckInsDarkPreview() = PublicCheckInsPreview(true)
