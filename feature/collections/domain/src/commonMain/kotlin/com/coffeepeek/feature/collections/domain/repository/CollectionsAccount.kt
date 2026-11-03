package com.coffeepeek.feature.collections.domain.repository

import kotlinx.coroutines.flow.Flow

/** Stable account identity only; credentials remain in application composition. */
interface CollectionsAccount {
    val identities: Flow<String?>
    fun currentId(): String?
    suspend fun loadId(): String?
}
