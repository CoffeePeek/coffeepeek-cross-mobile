package com.coffeepeek.feature.collections.domain.repository

import kotlinx.coroutines.flow.Flow

/** Stable account identity only; credentials remain in application composition. */
interface CollectionsAccount {
    val identities: Flow<String?>
    fun currentId(): String?
    suspend fun loadId(): String?
    /** Opaque media lifetime; token rotation may retain it, a new login must replace it. */
    fun mediaScope(): String? = currentId()
    val mediaScopes: Flow<String?> get() = identities
}
