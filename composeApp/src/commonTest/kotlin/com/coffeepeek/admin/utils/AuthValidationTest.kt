package com.coffeepeek.admin.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The login-side validators and the error mapping, pinned word by word before they move. */
class AuthValidationTest {
    private val credentials = "Неверный email или пароль"
    private val network = "Не удалось подключиться. Проверьте интернет-соединение"
    private val generic = "Не удалось войти. Проверьте email и пароль"

    @Test fun emailIsRequiredTrimmedAndMatchedAgainstASimplePattern() {
        assertEquals("Введите email", validateEmailRequired(""))
        assertEquals("Введите email", validateEmailRequired("   "))
        assertNull(validateEmailRequired(" person@example.com "))
        assertNull(validateEmailRequired("a.b+tag@sub.example.co"))
        for (bad in listOf("person", "person@", "@example.com", "person@example", "person@example.c", "per son@example.com", "person@@example.com")) {
            assertEquals("Введите корректный email", validateEmailRequired(bad), bad)
        }
    }

    @Test fun loginPasswordNeedsSixCharactersAndIsNotTrimmed() {
        assertEquals("Введите пароль", validatePasswordRequired(""))
        assertEquals("Введите пароль", validatePasswordRequired("      "))
        assertEquals("Пароль должен содержать как минимум 6 символов", validatePasswordRequired("12345"))
        assertNull(validatePasswordRequired("123456"))
        assertNull(validatePasswordRequired(" 12345 "), "Spaces count: the password is sent as typed")
    }

    @Test fun credentialLikeMessagesBecomeWrongEmailOrPassword() {
        for (message in listOf(
            "Unauthorized", "Invalid credentials", "invalid login", "Incorrect password", "wrong password", "User not found",
            "Неверные данные", "Неправильный пароль", "Пользователь не найден",
        )) assertEquals(credentials, localizedLoginError(message), message)
    }

    @Test fun networkLikeMessagesBecomeAConnectionHint() {
        for (message in listOf(
            "timeout", "Request timed out", "Network is unreachable", "Failed to connect to /10.0.0.1", "Socket closed",
            "Unable to resolve host", "No internet", "Сеть недоступна", "Проблемы с соединением",
        )) assertEquals(network, localizedLoginError(message), message)
    }

    @Test fun credentialWordsWinOverNetworkWordsAndEverythingElseIsGeneric() {
        assertEquals(credentials, localizedLoginError("Unauthorized: connection reset"))
        assertEquals(generic, localizedLoginError(null))
        assertEquals(generic, localizedLoginError(""))
        assertEquals(generic, localizedLoginError("Unexpected server response"))
        assertEquals(generic, localizedLoginError("Internal Server Error"))
        // Characterized quirk: the Russian stem is "сеть", so "Нет сети" does not match and gets the generic text.
        assertEquals(generic, localizedLoginError("Нет сети"))
    }
}
