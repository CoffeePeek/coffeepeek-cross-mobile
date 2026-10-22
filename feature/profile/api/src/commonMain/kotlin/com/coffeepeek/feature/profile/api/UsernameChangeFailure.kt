package com.coffeepeek.feature.profile.api

/** Stable failures without server prose. [retryAtUtc] is an optional ISO-8601 instant supplied by the server. */
class UsernameChangeFailure(
    val reason: Reason,
    val retryAtUtc: String? = null,
) : Exception() {
    enum class Reason { LimitReached, Taken, Invalid, SignInRequired, Unavailable }
}
