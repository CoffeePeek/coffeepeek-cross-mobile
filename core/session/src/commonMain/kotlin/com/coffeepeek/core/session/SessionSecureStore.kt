package com.coffeepeek.core.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/** For targets and tests without secure storage; nothing is persisted. */
class InMemorySessionSecureStore(initial: SessionCredentials? = null) : SessionSecureStore {
    private val state = MutableStateFlow(initial)
    override suspend fun read(): SessionCredentials? = state.value
    override suspend fun write(data: SessionCredentials?) { state.value = data }
    override fun observe(): Flow<SessionCredentials?> = state.asStateFlow()
}
