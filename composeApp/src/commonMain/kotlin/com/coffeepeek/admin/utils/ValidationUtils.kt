package com.coffeepeek.admin.utils

private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
private val PHONE_REGEX = "^[+]?[\\d\\s()-]{7,20}$".toRegex()
private val URL_REGEX =
    "^(https?://)?([a-z0-9-]+\\.)+[a-z]{2,}([/?#].*)?$".toRegex(RegexOption.IGNORE_CASE)

sealed interface InputValidationIssue {
    data object EmailRequired : InputValidationIssue
    data object EmailInvalid : InputValidationIssue
    data object PasswordRequired : InputValidationIssue
    data class PasswordTooShort(val minLength: Int) : InputValidationIssue
    data object PhoneInvalid : InputValidationIssue
    data object UrlInvalid : InputValidationIssue
    data object InstagramInvalid : InputValidationIssue
}

const val MIN_REGISTRATION_PASSWORD_LENGTH = 8

fun validateEmailRequired(email: String): InputValidationIssue? = when {
    email.isBlank() -> InputValidationIssue.EmailRequired
    !EMAIL_REGEX.matches(email.trim()) -> InputValidationIssue.EmailInvalid
    else -> null
}

fun validatePasswordRequired(password: String, minLength: Int = 6): InputValidationIssue? = when {
    password.isBlank() -> InputValidationIssue.PasswordRequired
    password.length < minLength -> InputValidationIssue.PasswordTooShort(minLength)
    else -> null
}

fun validateOptionalEmail(email: String): InputValidationIssue? {
    val value = email.trim()
    if (value.isEmpty()) return null
    return if (EMAIL_REGEX.matches(value)) null else InputValidationIssue.EmailInvalid
}

fun validateOptionalPhone(phone: String): InputValidationIssue? {
    val value = phone.trim()
    if (value.isEmpty()) return null
    return if (PHONE_REGEX.matches(value)) null else InputValidationIssue.PhoneInvalid
}

fun validateOptionalUrl(url: String): InputValidationIssue? {
    val value = url.trim()
    if (value.isEmpty()) return null
    return if (URL_REGEX.matches(value)) null else InputValidationIssue.UrlInvalid
}

fun validateOptionalInstagram(instagram: String): InputValidationIssue? {
    val value = instagram.trim()
    if (value.isEmpty()) return null
    val normalized = value.removePrefix("@")
    return if (normalized.matches("^[A-Za-z0-9._]{2,30}$".toRegex())) null else InputValidationIssue.InstagramInvalid
}
