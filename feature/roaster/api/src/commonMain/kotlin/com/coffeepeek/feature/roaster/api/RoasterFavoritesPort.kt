package com.coffeepeek.feature.roaster.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** What the favorites store needs to remember about a roaster. [key] is the public slug. */
data class RoasterFavoriteTarget(
    val key: String,
    val name: String,
    val photoUrl: String?,
    /** Public address parts; the favorites store persists the full address. */
    val canonicalPath: String,
    val revision: Int,
    val isAlias: Boolean,
)

/**
 * Server favorites of the active account. The application owns authentication gating, in-flight de-duplication,
 * account guards and analytics; the feature only asks and reacts.
 */
interface RoasterFavoritesPort {
    /** Favorite keys of the active account. */
    fun observeKeys(): Flow<Set<String>>
    /** Store-level error messages, null when cleared. */
    fun observeErrors(): Flow<String?>
    /** Fire and forget; [onFailed] runs only when a started change did not complete. */
    fun change(target: RoasterFavoriteTarget, favorite: Boolean, onFailed: () -> Unit)

    companion object {
        val None: RoasterFavoritesPort = object : RoasterFavoritesPort {
            override fun observeKeys(): Flow<Set<String>> = emptyFlow()
            override fun observeErrors(): Flow<String?> = emptyFlow()
            override fun change(target: RoasterFavoriteTarget, favorite: Boolean, onFailed: () -> Unit) = Unit
        }
    }
}
