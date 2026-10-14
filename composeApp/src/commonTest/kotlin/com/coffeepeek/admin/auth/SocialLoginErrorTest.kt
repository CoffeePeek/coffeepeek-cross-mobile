package com.coffeepeek.admin.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class SocialLoginErrorTest {
    @Test
    fun googleSdkDiagnosticCodeSurvivesFriendlyErrorMapping() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз. (код GSI-E-4)",
            socialLoginError("Google", "Не удалось войти через Google. Попробуйте ещё раз. [GSI-E-4]"),
        )
    }

    @Test
    fun googleErrorDoesNotExposeUnrecognizedDetails() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз.",
            socialLoginError("Google", "private details [GSI-E-12345678901]"),
        )
    }

    @Test
    fun appleErrorIgnoresGoogleDiagnosticCode() {
        assertEquals(
            "Не удалось войти через Apple. Попробуйте ещё раз.",
            socialLoginError("Apple", "[GSI-T]"),
        )
    }
}
