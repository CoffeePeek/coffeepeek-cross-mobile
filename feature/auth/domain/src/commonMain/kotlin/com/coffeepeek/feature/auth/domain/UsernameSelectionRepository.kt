package com.coffeepeek.feature.auth.domain

import kotlinx.coroutines.flow.Flow

data class UsernameSelectionOwner(val id: String, val generation: Long)
data class UsernameSelectionProfile(val username: String, val required: Boolean)

interface UsernameSelectionAccount {
    fun current(): UsernameSelectionOwner?
    fun observe(): Flow<UsernameSelectionOwner?>
}

interface UsernameSelectionRepository {
    /** Also synchronizes token name claims when setup is already complete. */
    suspend fun load(owner: UsernameSelectionOwner): Result<UsernameSelectionProfile>
    suspend fun complete(owner: UsernameSelectionOwner, username: String): Result<UsernameSelectionProfile>
}

enum class UsernameSelectionFailure { Invalid, Taken, Conflict, Unavailable }
class UsernameSelectionException(val reason: UsernameSelectionFailure) : Exception()

/** Matches the server's UTF-16 length and Unicode letter/digit rules; trim before sending. */
fun isValidSelectedUsername(value: String): Boolean {
    val name = value.trim()
    return name.length in 3..30 && name.first().isLetter() &&
        name.all { it.isLetterOrDigit() || it == '.' || it == '_' }
}
