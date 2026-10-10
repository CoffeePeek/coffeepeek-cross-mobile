package com.coffeepeek.feature.favorites.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Public Navigation 3 destination; no repository, VM or implementation in route arguments. */
@Serializable
data object FavoritesRoute : NavKey
