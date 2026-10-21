package com.coffeepeek.core.session

import kotlinx.coroutines.flow.Flow

/** Read-only view of the signed-in account: what features and their adapters need. */
interface SessionReader {
    /** In-memory snapshot; null until the stored session is loaded or when signed out. */
    fun peekSession(): Session?

    fun isActiveSession(session: Session?): Boolean

    fun observeSession(): Flow<Session?>
}

/** Read and write access, for sign-in, token refresh, sign-out and the transport layer only. */
interface SessionStore : SessionReader {
    fun applySession(session: Session?)

    suspend fun getSession(): Session?
    suspend fun persistSession(session: Session?)

    /** Atomically replace credentials only while the expected session is current. */
    suspend fun replaceSession(expected: Session?, session: Session?): Boolean {
        if (peekSession() != expected) return false
        saveSession(session)
        return true
    }

    suspend fun saveSession(session: Session?)
    suspend fun warmCache()
    suspend fun isLoggedIn(): Boolean
}
