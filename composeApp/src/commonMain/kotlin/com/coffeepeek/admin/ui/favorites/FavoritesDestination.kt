package com.coffeepeek.admin.ui.favorites

import androidx.compose.runtime.Composable

/** Keeps the shared root route while Android and iOS use different favorites screens. */
@Composable
internal expect fun FavoritesDestination()
