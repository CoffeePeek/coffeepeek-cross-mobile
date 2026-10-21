package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun peekSession(): Session?

    fun applySession(session: Session?)

    fun isActiveSession(session: Session?): Boolean

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
    fun observeSession(): Flow<Session?>
    suspend fun isLoggedIn(): Boolean
}
