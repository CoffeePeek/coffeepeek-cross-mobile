package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.compose.component.InlineProfileError
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionHeading
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionLoader
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState

/** Private fallback for an owner opening their own public-profile destination. */
@Composable
internal fun OwnProfileFavoritesPreview(state: PublicProfileState, slots: ProfileSlots,
    onOpen: (PublicProfileFavorite) -> Unit, onAll: () -> Unit, onRetry: () -> Unit) {
    val favorites = state.profile?.favorites.orEmpty()
    if (favorites.isEmpty() && !state.isLoadingFavorites && state.favoritesError == null) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            ProfileSectionHeading(stringResource(Res.string.profile_section_favorites), onAll = onAll)
        }
        when {
            state.isLoadingFavorites && favorites.isEmpty() -> ProfileSectionLoader()
            state.favoritesError != null && favorites.isEmpty() -> Box(Modifier.padding(horizontal = 16.dp)) {
                InlineProfileError(stringResource(Res.string.profile_favorites_failed), onRetry)
            }
            else -> {
                slots.favoritesPreview(favorites.take(6), onOpen)
                if (state.favoritesError != null) Box(Modifier.padding(horizontal = 16.dp)) {
                    InlineProfileError(stringResource(Res.string.profile_favorites_refresh_failed), onRetry)
                }
            }
        }
    }
}

@Composable
private fun OwnFavoritesPreviewSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val favorite = PublicProfileFavorite("sample", "CoffeePeek", PublicFavoriteKind.CoffeeShop)
    val state = PublicProfileState(profile = PublicProfile("sample", "CoffeePeek", null, null,
        PublicProfileStats(0, 0, 0, 0), listOf(favorite), null))
    val slots = ProfileSlots(image = { _, _, _ -> }, photoViewer = { _, _, _, _ -> },
        favoritesPreview = { values, _ -> values.forEach { Text(it.title) } }, favoriteCard = { _, _, _ -> },
        checkInCard = { _, _, _, _, _ -> }, photoPrefetch = { _, _ -> }, activityPreview = { _, _, _, _, _, _, _ -> })
    OwnProfileFavoritesPreview(state, slots, {}, {}, {})
}

@Preview @Composable private fun OwnFavoritesLightPreview() = OwnFavoritesPreviewSample(false)
@Preview @Composable private fun OwnFavoritesDarkPreview() = OwnFavoritesPreviewSample(true)
