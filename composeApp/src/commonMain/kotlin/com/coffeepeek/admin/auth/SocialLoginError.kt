package com.coffeepeek.admin.auth

private val googleSignInDiagnosticCode = Regex("""\[(GSI-(?:E-?[0-9]{1,10}|R|T))(?: D:([A-Za-z0-9._-]{1,80}))?(?: O:([A-Za-z][A-Za-z0-9_]{0,39}))?]""")

internal fun socialLoginError(provider: String, message: String?): String {
    val normalized = message.orEmpty().lowercase()
    val userMessage = when {
        "already" in normalized && ("email" in normalized || "account" in normalized) ->
            "Аккаунт с этим email уже существует. Используйте способ входа, с которым вы регистрировались."
        "verified email" in normalized || "email is not verified" in normalized ->
            "$provider не передал подтверждённый email. Разрешите доступ к email и попробуйте ещё раз."
        listOf("timeout", "timed out", "network", "connect", "socket", "host").any(normalized::contains) ->
            "Не удалось подключиться. Проверьте интернет-соединение"
        else -> "Не удалось войти через $provider. Попробуйте ещё раз."
    }
    val diagnostic = if (provider == "Google") {
        googleSignInDiagnosticCode.find(message.orEmpty())
    } else {
        null
    }
    if (diagnostic == null) return userMessage
    val domain = diagnostic.groupValues[2].takeIf(String::isNotEmpty)
    val domainSuffix = domain?.let { "; домен $it" }.orEmpty()
    val oauthError = diagnostic.groupValues[3].takeIf(String::isNotEmpty)
    val oauthErrorSuffix = oauthError?.let { "; OAuth $it" }.orEmpty()
    return "$userMessage (код ${diagnostic.groupValues[1]}$domainSuffix$oauthErrorSuffix)"
}
