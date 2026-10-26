package com.coffeepeek.feature.auth.domain

const val MIN_LOGIN_PASSWORD_LENGTH = 6
const val MIN_REGISTRATION_PASSWORD_LENGTH = 8
const val MIN_REGISTRATION_NAME_LENGTH = 2

private val EmailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

enum class EmailIssue { Empty, Invalid }

sealed interface PasswordIssue {
    data object Empty : PasswordIssue
    data class TooShort(val minLength: Int) : PasswordIssue
}

fun validateEmail(email: String): EmailIssue? = when {
    email.isBlank() -> EmailIssue.Empty
    !EmailPattern.matches(email.trim()) -> EmailIssue.Invalid
    else -> null
}

/** Whitespace counts: the password is sent exactly as typed. */
fun validatePassword(password: String, minLength: Int): PasswordIssue? = when {
    password.isBlank() -> PasswordIssue.Empty
    password.length < minLength -> PasswordIssue.TooShort(minLength)
    else -> null
}

/** The first registration step can be submitted as soon as something is typed. */
fun canContinueWithEmail(email: String): Boolean = email.isNotBlank()

/** The submit button of the details step is enabled only when this holds. */
fun canSubmitRegistration(name: String, password: String, termsAccepted: Boolean): Boolean =
    name.trim().length >= MIN_REGISTRATION_NAME_LENGTH && password.length >= MIN_REGISTRATION_PASSWORD_LENGTH && termsAccepted

/** What a registration submit reports. Each failing check overwrote the previous message, so the last one in the order terms, password, email, name wins. */
enum class RegistrationIssue { NameRequired, EmailRequired, EmailInvalid, PasswordTooShort, TermsRequired }

data class RegistrationCheck(
    val email: EmailIssue?,
    val password: PasswordIssue?,
    val nameMissing: Boolean,
    val termsMissing: Boolean,
) {
    val valid: Boolean get() = email == null && password == null && !nameMissing && !termsMissing

    /** The single message the submit shows, or null when everything is valid. */
    val reported: RegistrationIssue?
        get() = when {
            nameMissing -> RegistrationIssue.NameRequired
            email == EmailIssue.Empty -> RegistrationIssue.EmailRequired
            email == EmailIssue.Invalid -> RegistrationIssue.EmailInvalid
            password != null -> RegistrationIssue.PasswordTooShort
            termsMissing -> RegistrationIssue.TermsRequired
            else -> null
        }
}

fun checkRegistration(name: String, email: String, password: String, termsAccepted: Boolean) = RegistrationCheck(
    email = validateEmail(email.trim()),
    password = validatePassword(password, MIN_REGISTRATION_PASSWORD_LENGTH),
    nameMissing = name.trim().isEmpty(),
    termsMissing = !termsAccepted,
)

/** Shown under the registration password: one point each for 8+ characters, a digit and an upper-case letter. */
enum class PasswordStrength(val score: Int) { None(0), Weak(1), Medium(2), Strong(3) }

fun passwordStrength(password: String): PasswordStrength {
    val score = (if (password.length >= 8) 1 else 0) +
        (if (password.any { it.isDigit() }) 1 else 0) +
        (if (password.any { it.isUpperCase() }) 1 else 0)
    return PasswordStrength.entries.first { it.score == score }
}
