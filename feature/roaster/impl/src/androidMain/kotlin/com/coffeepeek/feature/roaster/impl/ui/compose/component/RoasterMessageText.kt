package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.coffeepeek.feature.roaster.impl.R
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage

@Composable
internal fun RoasterMessage.text(): String = when (this) {
    RoasterMessage.FavoritesUnavailable -> stringResource(R.string.roaster_message_favorites_unavailable)
    RoasterMessage.FavoriteChangeFailed -> stringResource(R.string.roaster_message_favorite_failed)
    is RoasterMessage.FromStore -> text
}
