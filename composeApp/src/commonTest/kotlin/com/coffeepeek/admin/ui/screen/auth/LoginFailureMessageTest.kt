package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.feature.auth.domain.LoginFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginFailureMessageTest {
    private val russian = LoginFailureCopy("Неверный email или пароль",
        "Не удалось подключиться. Проверьте интернет-соединение", "Не удалось войти. Проверьте email и пароль")
    private val belarusian = LoginFailureCopy("Няправільны email або пароль",
        "Не ўдалося падключыцца. Праверце інтэрнэт-злучэнне", "Не ўдалося ўвайсці. Праверце email і пароль")

    @Test fun aRetainedFailureRendersWithTheCurrentLanguageWithoutAnotherRequest() {
        val failures = listOf(LoginFailure.WrongCredentials, LoginFailure.Network, LoginFailure.Unknown)
        assertEquals(listOf(russian.wrongCredentials, russian.network, russian.unknown), failures.map { it.message(russian) })
        assertEquals(listOf(belarusian.wrongCredentials, belarusian.network, belarusian.unknown), failures.map { it.message(belarusian) })
    }
}
