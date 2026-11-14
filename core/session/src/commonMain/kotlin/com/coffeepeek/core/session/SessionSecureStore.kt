package com.coffeepeek.core.session

import kotlinx.coroutines.flow.Flow

interface SessionSecureStore {
    suspend fun read(): SessionCredentials?
    suspend fun write(data: SessionCredentials?)
    fun observe(): Flow<SessionCredentials?>
}

/** Rows from the pre-secure-store era that are imported once and then deleted. */
interface LegacyCredentialRows {
    suspend fun read(key: String): SessionCredentials?
    suspend fun delete(key: String)

    companion object {
        const val SESSION = "SESSION"
        const val API_KEYS = "API_KEYS"
    }
}

