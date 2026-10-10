package com.coffeepeek.feature.favorites.data.local

import kotlinx.coroutines.flow.Flow

/** Construction port. Observe must emit current data and later successful writes/clears. */
interface FavoritesStorage {
    suspend fun read(key: String): String?
    fun observe(key: String): Flow<String?>
    /** Null deletes the key. A failed write must throw rather than report success. */
    suspend fun write(key: String, value: String?)
}
