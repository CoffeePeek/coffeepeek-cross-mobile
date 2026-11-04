package com.coffeepeek.feature.favorites.domain.repository

import kotlinx.coroutines.flow.Flow

/** Active favorite owner; replay the current identity before subsequent changes. Null means signed out. */
interface FavoritesAccount {
    val identities: Flow<String?>
    fun currentId(): String?
}
