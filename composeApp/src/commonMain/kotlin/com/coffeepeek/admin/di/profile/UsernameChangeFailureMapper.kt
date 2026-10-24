package com.coffeepeek.admin.di.profile

import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import kotlinx.coroutines.CancellationException

/** Translates the legacy server contract at the composition bridge until profile writes migrate. */
internal fun Throwable.toUsernameChangeFailure(): UsernameChangeFailure {
    if (this is CancellationException) throw this
    if (this is UsernameChangeFailure) return this
    val text = message.orEmpty().lowercase()
    val status = (this as? ApiException)?.statusCode
    val reason = when {
        status == 401 || status == 403 -> UsernameChangeFailure.Reason.SignInRequired
        status == 429 || text.isUsernameLimit() -> UsernameChangeFailure.Reason.LimitReached
        listOf("already taken", "already exists", "already in use", "занят").any(text::contains) ->
            UsernameChangeFailure.Reason.Taken
        status == 409 -> UsernameChangeFailure.Reason.Taken
        status == 400 || status == 422 -> UsernameChangeFailure.Reason.Invalid
        else -> UsernameChangeFailure.Reason.Unavailable
    }
    return UsernameChangeFailure(reason, RETRY_INSTANT.find(message.orEmpty())?.value)
}

private fun String.isUsernameLimit(): Boolean =
    (contains("username") || contains("user name") || contains("ник") || contains("имя")) &&
        listOf("once every", "per week", "times a week", "7 days", "limit", "недел", "лимит", "раз в").any(::contains)

private val RETRY_INSTANT = Regex(
    """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})""",
)
