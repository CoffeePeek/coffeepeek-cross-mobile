package com.coffeepeek.admin.auth

private val googleSignInDiagnosticCode = Regex("""\[(GSI-(?:E-?[0-9]{1,10}|R|T))]""")

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
    val diagnosticCode = if (provider == "Google") {
        googleSignInDiagnosticCode.find(message.orEmpty())?.groupValues?.get(1)
    } else {
        null
    }
    return if (diagnosticCode == null) userMessage else "$userMessage (код $diagnosticCode)"
}
