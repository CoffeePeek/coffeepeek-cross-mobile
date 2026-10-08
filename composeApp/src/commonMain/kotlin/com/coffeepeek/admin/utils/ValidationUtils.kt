package com.coffeepeek.admin.utils

private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
private val PHONE_REGEX = "^[+]?[\\d\\s()-]{7,20}$".toRegex()
private val URL_REGEX =
    "^(https?://)?([a-z0-9-]+\\.)+[a-z]{2,}([/?#].*)?$".toRegex(RegexOption.IGNORE_CASE)

// Сообщения синхронизированы с composeResources/values/strings.xml
private const val MSG_EMAIL_REQUIRED = "Введите email"
private const val MSG_EMAIL_INVALID = "Введите корректный email"
private const val MSG_PASSWORD_REQUIRED = "Введите пароль"
private const val MSG_PHONE_INVALID = "Некорректный формат телефона"
private const val MSG_URL_INVALID = "Укажите домен, например coffeepeek.by"
private const val MSG_INSTAGRAM_INVALID = "Некорректный Instagram"

const val MIN_REGISTRATION_PASSWORD_LENGTH = 8

fun validateEmailRequired(email: String): String? = when {
    email.isBlank() -> MSG_EMAIL_REQUIRED
    !EMAIL_REGEX.matches(email.trim()) -> MSG_EMAIL_INVALID
    else -> null
}

fun validatePasswordRequired(password: String, minLength: Int = 6): String? = when {
    password.isBlank() -> MSG_PASSWORD_REQUIRED
    password.length < minLength -> "Пароль должен содержать как минимум $minLength символов"
    else -> null
}

fun localizedLoginError(message: String?): String {
    val normalized = message.orEmpty().lowercase()
    return when {
        listOf(
            "unauthorized",
            "invalid credential",
            "invalid login",
            "incorrect password",
            "wrong password",
            "user not found",
            "неверн",
            "неправильн",
            "не найден",
        ).any(normalized::contains) -> "Неверный email или пароль"

        listOf(
            "timeout",
            "timed out",
            "network",
            "connect",
            "socket",
            "host",
            "internet",
            "сеть",
            "соединен",
        ).any(normalized::contains) -> "Не удалось подключиться. Проверьте интернет-соединение"

        else -> "Не удалось войти. Проверьте email и пароль"
    }
}

fun validateOptionalEmail(email: String): String? {
    val value = email.trim()
    if (value.isEmpty()) return null
    return if (EMAIL_REGEX.matches(value)) null else MSG_EMAIL_INVALID
}

fun validateOptionalPhone(phone: String): String? {
    val value = phone.trim()
    if (value.isEmpty()) return null
    return if (PHONE_REGEX.matches(value)) null else MSG_PHONE_INVALID
}

fun validateOptionalUrl(url: String): String? {
    val value = url.trim()
    if (value.isEmpty()) return null
    return if (URL_REGEX.matches(value)) null else MSG_URL_INVALID
}

fun validateOptionalInstagram(instagram: String): String? {
    val value = instagram.trim()
    if (value.isEmpty()) return null
    val normalized = value.removePrefix("@")
    return if (normalized.matches("^[A-Za-z0-9._]{2,30}$".toRegex())) null else MSG_INSTAGRAM_INVALID
}
