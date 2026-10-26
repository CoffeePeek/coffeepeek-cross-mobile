package com.coffeepeek.feature.profile.data.backend

import com.coffeepeek.feature.profile.api.UsernameChangeFailure

/** Preserves the existing username failure contract while keeping server prose out of presentation. */
internal fun usernameFailure(status: Int, message: String?): UsernameChangeFailure {
    val text = message.orEmpty().lowercase()
    val reason = when {
        status == 401 || status == 403 -> UsernameChangeFailure.Reason.SignInRequired
        status == 429 || text.isUsernameLimit() -> UsernameChangeFailure.Reason.LimitReached
        listOf("already taken", "already exists", "already in use", "занят").any(text::contains) -> UsernameChangeFailure.Reason.Taken
        status == 409 -> UsernameChangeFailure.Reason.Taken
        status == 400 || status == 422 -> UsernameChangeFailure.Reason.Invalid
        else -> UsernameChangeFailure.Reason.Unavailable
    }
    return UsernameChangeFailure(reason, retryInstant.find(message.orEmpty())?.value)
}

private fun String.isUsernameLimit() =
    (contains("username") || contains("user name") || contains("ник") || contains("имя")) &&
        listOf("once every", "per week", "times a week", "7 days", "limit", "недел", "лимит", "раз в").any(::contains)

private val retryInstant = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})""")
