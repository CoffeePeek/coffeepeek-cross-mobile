package com.coffeepeek.feature.auth.domain

/** How a failed e-mail sign-in is told to the user. The classification works on the message text, as it always did. */
enum class LoginFailure { WrongCredentials, Network, Unknown }

private val credentialWords = listOf(
    "unauthorized", "invalid credential", "invalid login", "incorrect password", "wrong password", "user not found",
    "неверн", "неправильн", "не найден",
)
private val networkWords = listOf("timeout", "timed out", "network", "connect", "socket", "host", "internet", "сеть", "соединен")

fun classifyLoginFailure(message: String?): LoginFailure {
    val normalized = message.orEmpty().lowercase()
    return when {
        credentialWords.any(normalized::contains) -> LoginFailure.WrongCredentials
        networkWords.any(normalized::contains) -> LoginFailure.Network
        else -> LoginFailure.Unknown
    }
}

enum class SocialProvider { Google, Apple }

enum class SocialFailureKind { AccountExists, EmailNotVerified, Network, Generic }

/** A provider diagnostic code found in the message, for example `GSI-E10` with its optional domain and OAuth error. */
data class SocialDiagnostic(val code: String, val domain: String?, val oauthError: String?)

data class SocialFailure(val kind: SocialFailureKind, val diagnostic: SocialDiagnostic?)

private val googleDiagnostic = Regex("""\[(GSI-(?:E-?[0-9]{1,10}|R|T))(?: D:([A-Za-z0-9._-]{1,80}))?(?: O:([A-Za-z][A-Za-z0-9_]{0,39}))?]""")
private val appleDiagnostic = Regex("""\[(ASI-(?:E-?[0-9]{1,10}|H[1-5][0-9]{2}|CH|X|W|C|S|T|B|R|A|I|P))(?: D:([A-Za-z0-9._-]{1,80}))?]""")

fun classifySocialFailure(provider: SocialProvider, message: String?): SocialFailure {
    val normalized = message.orEmpty().lowercase()
    val kind = when {
        "already" in normalized && ("email" in normalized || "account" in normalized) -> SocialFailureKind.AccountExists
        "verified email" in normalized || "email is not verified" in normalized -> SocialFailureKind.EmailNotVerified
        listOf("timeout", "timed out", "network", "connect", "socket", "host").any(normalized::contains) -> SocialFailureKind.Network
        else -> SocialFailureKind.Generic
    }
    val found = when (provider) {
        SocialProvider.Google -> googleDiagnostic.find(message.orEmpty())
        SocialProvider.Apple -> appleDiagnostic.find(message.orEmpty())
    }
    val diagnostic = found?.let {
        SocialDiagnostic(
            code = it.groupValues[1],
            domain = it.groupValues[2].takeIf(String::isNotEmpty),
            oauthError = it.groupValues.getOrNull(3)?.takeIf(String::isNotEmpty),
        )
    }
    return SocialFailure(kind, diagnostic)
}
