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
    fun googleSdkDiagnosticDomainSurvivesFriendlyErrorMapping() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз. (код GSI-E-61440; домен com.google.GIDSignIn)",
            socialLoginError("Google", "private details [GSI-E-61440 D:com.google.GIDSignIn]"),
        )
    }

    @Test
    fun googleOAuthIdentifierSurvivesWithoutResponseDetails() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз. (код GSI-E-61440; домен org.openid.appauth.oauth_token; OAuth invalid_rapt)",
            socialLoginError(
                "Google",
                "private details [GSI-E-61440 D:org.openid.appauth.oauth_token O:invalid_rapt]",
            ),
        )
    }

    @Test
    fun googleOAuthIdentifierRejectsResponseDescription() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз.",
            socialLoginError("Google", "[GSI-E-61440 O:invalid_rapt;token=private]"),
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
    fun googleErrorDoesNotExposeUntrustedDomain() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз.",
            socialLoginError("Google", "private details [GSI-E-61440 D:com.google;token=private]"),
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
