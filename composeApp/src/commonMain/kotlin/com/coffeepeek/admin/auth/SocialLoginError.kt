package com.coffeepeek.admin.auth

private val googleSignInDiagnosticCode = Regex("""\[(GSI-(?:E-?[0-9]{1,10}|R|T))(?: D:([A-Za-z0-9._-]{1,80}))?(?: O:([A-Za-z][A-Za-z0-9_]{0,39}))?]""")
private val appleSignInDiagnosticCode = Regex("""\[(ASI-(?:E-?[0-9]{1,10}|H[1-5][0-9]{2}|CH|X|W|C|S|T|B|R|A|I|P))(?: D:([A-Za-z0-9._-]{1,80}))?]""")

internal fun socialLoginError(provider: String, message: String?, copy: SocialLoginErrorCopy): String {
    val userMessage = socialFriendlyMessage(provider, message, copy)
    val diagnostic = when (provider) {
        "Google" -> googleSignInDiagnosticCode.find(message.orEmpty())
        "Apple" -> appleSignInDiagnosticCode.find(message.orEmpty())
        else -> null
    }
    return if (diagnostic == null) userMessage else socialDiagnosticMessage(userMessage, diagnostic, copy)
}

private fun socialFriendlyMessage(provider: String, message: String?, copy: SocialLoginErrorCopy): String {
    val normalized = message.orEmpty().lowercase()
    return when {
        "already" in normalized && ("email" in normalized || "account" in normalized) -> copy.accountExists
        "verified email" in normalized || "email is not verified" in normalized -> copy.emailUnverified.replace("%1\$s", provider)
        listOf("timeout", "timed out", "network", "connect", "socket", "host").any(normalized::contains) -> copy.network
        else -> copy.generic.replace("%1\$s", provider)
    }
}

private fun socialDiagnosticMessage(message: String, diagnostic: MatchResult, copy: SocialLoginErrorCopy): String {
    val domain = diagnostic.groupValues[2].takeIf(String::isNotEmpty)
    val domainSuffix = domain?.let { "; ${copy.domainLabel} $it" }.orEmpty()
    val oauth = diagnostic.groupValues.getOrNull(3)?.takeIf(String::isNotEmpty)
    val oauthSuffix = oauth?.let { "; ${copy.oauthLabel} $it" }.orEmpty()
    return "$message (${copy.codeLabel} ${diagnostic.groupValues[1]}$domainSuffix$oauthSuffix)"
}
