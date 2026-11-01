package com.coffeepeek.admin.utils

import com.coffeepeek.admin.ui.screen.auth.message

import com.coffeepeek.admin.ui.screen.auth.LoginFailureCopy

import com.coffeepeek.feature.auth.domain.classifyLoginFailure

import com.coffeepeek.admin.ui.component.message

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PasswordValidationTest {
    private val loginCopy = LoginFailureCopy("Неверный email или пароль", "Не удалось подключиться. Проверьте интернет-соединение", "Не удалось войти. Проверьте email и пароль")

    private val labels = com.coffeepeek.admin.ui.component.InputValidationLabels("Введите email", "Введите корректный email", "Введите пароль", "Пароль должен содержать как минимум %1\$s символов", "Некорректный формат телефона", "Укажите домен, например coffeepeek.by", "Некорректный Instagram")


    @Test
    fun requiredAuthFieldsUseConciseRussianMessages() {
        assertEquals("Введите email", validateEmailRequired("")?.message(labels))
        assertEquals("Введите корректный email", validateEmailRequired("broken-address")?.message(labels))
        assertEquals("Введите пароль", validatePasswordRequired("")?.message(labels))
    }

    @Test
    fun registrationPasswordRequiresAtLeastEightCharacters() {
        assertEquals(
            "Пароль должен содержать как минимум 8 символов",
            validatePasswordRequired(
                password = "1234567",
                minLength = MIN_REGISTRATION_PASSWORD_LENGTH,
            )?.message(labels),
        )
        assertNull(
            validatePasswordRequired(
                password = "12345678",
                minLength = MIN_REGISTRATION_PASSWORD_LENGTH,
            )?.message(labels),
        )
    }

    @Test
    fun loginApiErrorsAreLocalized() {
        assertEquals(
            "Неверный email или пароль",
            classifyLoginFailure("Invalid credentials").message(loginCopy),
        )
        assertEquals(
            "Не удалось подключиться. Проверьте интернет-соединение",
            classifyLoginFailure("Connection timeout").message(loginCopy),
        )
        assertEquals(
            "Не удалось войти. Проверьте email и пароль",
            classifyLoginFailure("Unexpected server response").message(loginCopy),
        )
    }
    @Test fun preparedBelarusianPasswordLengthPreservesTheConfiguredMinimum() {
        val translated = labels.copy(passwordLength = "Не менш за %1\$s сімвалаў")
        assertEquals("Не менш за 8 сімвалаў", validatePasswordRequired("1234567", 8)?.message(translated))
    }
}
