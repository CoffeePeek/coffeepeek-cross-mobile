package com.coffeepeek.admin.auth

internal fun socialLoginError(provider: String, message: String?): String {
    val normalized = message.orEmpty().lowercase()
    return when {
        "already" in normalized && ("email" in normalized || "account" in normalized) ->
            "Аккаунт с этим email уже существует. Используйте способ входа, с которым вы регистрировались."
        "verified email" in normalized || "email is not verified" in normalized ->
            "$provider не передал подтверждённый email. Разрешите доступ к email и попробуйте ещё раз."
        listOf("timeout", "timed out", "network", "connect", "socket", "host").any(normalized::contains) ->
            "Не удалось подключиться. Проверьте интернет-соединение"
        else -> "Не удалось войти через $provider. Попробуйте ещё раз."
    }
}
