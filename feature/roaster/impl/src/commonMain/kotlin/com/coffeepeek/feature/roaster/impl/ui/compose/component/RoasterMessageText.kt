package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_message_favorite_failed
import com.coffeepeek.feature.roaster.impl.resources.roaster_message_favorites_unavailable
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage

@Composable
internal fun RoasterMessage.text(): String = when (this) {
    RoasterMessage.FavoritesUnavailable -> stringResource(Res.string.roaster_message_favorites_unavailable)
    RoasterMessage.FavoriteChangeFailed -> stringResource(Res.string.roaster_message_favorite_failed)
    is RoasterMessage.FromStore -> text
}
