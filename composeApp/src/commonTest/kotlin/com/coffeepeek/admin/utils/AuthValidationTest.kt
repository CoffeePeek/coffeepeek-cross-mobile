package com.coffeepeek.admin.utils

import com.coffeepeek.admin.ui.screen.auth.message

import com.coffeepeek.admin.ui.screen.auth.LoginFailureCopy

import com.coffeepeek.feature.auth.domain.classifyLoginFailure

import com.coffeepeek.admin.ui.component.message

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The login-side validators and the error mapping, pinned word by word before they move. */
class AuthValidationTest {
    private val loginCopy = LoginFailureCopy("Неверный email или пароль", "Не удалось подключиться. Проверьте интернет-соединение", "Не удалось войти. Проверьте email и пароль")

    private val labels = com.coffeepeek.admin.ui.component.InputValidationLabels("Введите email", "Введите корректный email", "Введите пароль", "Пароль должен содержать как минимум %1\$s символов", "Некорректный формат телефона", "Укажите домен, например coffeepeek.by", "Некорректный Instagram")

    private val credentials = "Неверный email или пароль"
    private val network = "Не удалось подключиться. Проверьте интернет-соединение"
    private val generic = "Не удалось войти. Проверьте email и пароль"

    @Test fun emailIsRequiredTrimmedAndMatchedAgainstASimplePattern() {
        assertEquals("Введите email", validateEmailRequired("")?.message(labels))
        assertEquals("Введите email", validateEmailRequired("   ")?.message(labels))
        assertNull(validateEmailRequired(" person@example.com ")?.message(labels))
        assertNull(validateEmailRequired("a.b+tag@sub.example.co")?.message(labels))
        for (bad in listOf("person", "person@", "@example.com", "person@example", "person@example.c", "per son@example.com", "person@@example.com")) {
            assertEquals("Введите корректный email", validateEmailRequired(bad)?.message(labels), bad)
        }
    }

    @Test fun loginPasswordNeedsSixCharactersAndIsNotTrimmed() {
        assertEquals("Введите пароль", validatePasswordRequired("")?.message(labels))
        assertEquals("Введите пароль", validatePasswordRequired("      ")?.message(labels))
        assertEquals("Пароль должен содержать как минимум 6 символов", validatePasswordRequired("12345")?.message(labels))
        assertNull(validatePasswordRequired("123456")?.message(labels))
        assertNull(validatePasswordRequired(" 12345 ")?.message(labels), "Spaces count: the password is sent as typed")
    }

    @Test fun credentialLikeMessagesBecomeWrongEmailOrPassword() {
        for (message in listOf(
            "Unauthorized", "Invalid credentials", "invalid login", "Incorrect password", "wrong password", "User not found",
            "Неверные данные", "Неправильный пароль", "Пользователь не найден",
        )) assertEquals(credentials, classifyLoginFailure(message).message(loginCopy), message)
    }

    @Test fun networkLikeMessagesBecomeAConnectionHint() {
        for (message in listOf(
            "timeout", "Request timed out", "Network is unreachable", "Failed to connect to /10.0.0.1", "Socket closed",
            "Unable to resolve host", "No internet", "Сеть недоступна", "Проблемы с соединением",
        )) assertEquals(network, classifyLoginFailure(message).message(loginCopy), message)
    }

    @Test fun credentialWordsWinOverNetworkWordsAndEverythingElseIsGeneric() {
        assertEquals(credentials, classifyLoginFailure("Unauthorized: connection reset").message(loginCopy))
        assertEquals(generic, classifyLoginFailure(null).message(loginCopy))
        assertEquals(generic, classifyLoginFailure("").message(loginCopy))
        assertEquals(generic, classifyLoginFailure("Unexpected server response").message(loginCopy))
        assertEquals(generic, classifyLoginFailure("Internal Server Error").message(loginCopy))
        // Characterized quirk: the Russian stem is "сеть", so "Нет сети" does not match and gets the generic text.
        assertEquals(generic, classifyLoginFailure("Нет сети").message(loginCopy))
    }
}
