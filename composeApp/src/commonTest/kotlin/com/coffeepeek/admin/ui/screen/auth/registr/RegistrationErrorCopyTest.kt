package com.coffeepeek.admin.ui.screen.auth.registr

import com.coffeepeek.feature.auth.domain.RegistrationIssue
import kotlin.test.Test
import kotlin.test.assertEquals

internal val russianRegistrationErrorCopy = RegistrationErrorCopy(
    checkFailed = "Попробуйте позже!",
    registrationFailed = "Ошибка при попытке создания учетной записи, попробуйте позже!",
    nameRequired = "Введите имя!", emailRequired = "Введите email", emailInvalid = "Введите корректный email",
    passwordTooShort = "Пароль должен содержать как минимум 8 символов",
    termsRequired = "Для завершения регистрации необходимо принять условия соглашения и политику конфиденциальности!",
)

internal val belarusianRegistrationErrorCopy = RegistrationErrorCopy(
    checkFailed = "Паспрабуйце пазней!",
    registrationFailed = "Памылка пры стварэнні ўліковага запісу, паспрабуйце пазней!",
    nameRequired = "Увядзіце імя!", emailRequired = "Увядзіце email", emailInvalid = "Увядзіце карэктны email",
    passwordTooShort = "Пароль мусіць змяшчаць прынамсі 8 сімвалаў",
    termsRequired = "Каб завяршыць рэгістрацыю, неабходна прыняць умовы пагаднення і палітыку прыватнасці!",
)

class RegistrationErrorCopyTest {
    @Test fun theSameValidationIssuesRenderFromTheCurrentRussianOrBelarusianCopy() {
        val issues = listOf(RegistrationIssue.NameRequired, RegistrationIssue.EmailRequired, RegistrationIssue.EmailInvalid,
            RegistrationIssue.PasswordTooShort, RegistrationIssue.TermsRequired)
        assertEquals(listOf(russianRegistrationErrorCopy.nameRequired, russianRegistrationErrorCopy.emailRequired,
            russianRegistrationErrorCopy.emailInvalid, russianRegistrationErrorCopy.passwordTooShort, russianRegistrationErrorCopy.termsRequired),
            issues.map { it.message(russianRegistrationErrorCopy) })
        assertEquals(listOf(belarusianRegistrationErrorCopy.nameRequired, belarusianRegistrationErrorCopy.emailRequired,
            belarusianRegistrationErrorCopy.emailInvalid, belarusianRegistrationErrorCopy.passwordTooShort, belarusianRegistrationErrorCopy.termsRequired),
            issues.map { it.message(belarusianRegistrationErrorCopy) })
    }
}
