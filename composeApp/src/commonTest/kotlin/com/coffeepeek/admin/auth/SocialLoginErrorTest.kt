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

    @Test
    fun appleSdkCodeAndDomainSurviveWithoutPrivateErrorDetails() {
        assertEquals(
            "Не удалось войти через Apple. Попробуйте ещё раз. (код ASI-E1000; домен com.apple.AuthenticationServices.AuthorizationError)",
            socialLoginError("Apple", "private-token=secret [ASI-E1000 D:com.apple.AuthenticationServices.AuthorizationError]"),
        )
    }

    @Test
    fun appleCallbackFailureStagesRemainDistinguishable() {
        for (code in listOf("W", "C", "S", "T", "B", "CH", "X", "R", "A", "I", "P")) {
            assertEquals(
                "Не удалось войти через Apple. Попробуйте ещё раз. (код ASI-$code)",
                socialLoginError("Apple", "[ASI-$code]"),
            )
        }
    }

    @Test
    fun appleSdkCodesCanBeNegativeWithoutAPlatformDomain() {
        assertEquals(
            "Не удалось войти через Apple. Попробуйте ещё раз. (код ASI-E-1000)",
            socialLoginError("Apple", "[ASI-E-1000]"),
        )
    }

    @Test
    fun appleErrorRejectsUnboundedCodesAndUntrustedDomainDetails() {
        for (message in listOf(
            "[ASI-E12345678901]",
            "[ASI-E1000 D:com.apple;token=secret]",
            "[ASI-E1000 D:${"a".repeat(81)}]",
            "[ASI-E1000 O:private]",
        )) {
            assertEquals("Не удалось войти через Apple. Попробуйте ещё раз.", socialLoginError("Apple", message))
        }
    }

    @Test
    fun googleErrorIgnoresAppleDiagnosticCodes() {
        assertEquals(
            "Не удалось войти через Google. Попробуйте ещё раз.",
            socialLoginError("Google", "[ASI-E1000]"),
        )
    }

    @Test
    fun appleStageDiagnosticsKeepExistingFriendlyNetworkErrors() {
        assertEquals(
            "Не удалось подключиться. Проверьте интернет-соединение (код ASI-X)",
            socialLoginError("Apple", "network unavailable [ASI-X]"),
        )
    }

    @Test
    fun appleHttpFailureIsVisibleThroughTheOuterExchangeStage() {
        for (status in listOf(400, 401, 403, 429, 500, 503)) {
            assertEquals(
                "Не удалось войти через Apple. Попробуйте ещё раз. (код ASI-H$status)",
                socialLoginError("Apple", "Apple login rejected [ASI-H$status] [ASI-X]"),
            )
        }
    }

    @Test
    fun appleResponseAndStorageFailuresRemainSpecificInsideTheExchangeStage() {
        for (code in listOf("R", "A", "I", "P")) {
            assertEquals(
                "Не удалось войти через Apple. Попробуйте ещё раз. (код ASI-$code)",
                socialLoginError("Apple", "[ASI-$code] [ASI-X]"),
            )
        }
    }

    @Test
    fun invalidHttpDiagnosticsCannotExposeArbitraryDetails() {
        for (code in listOf("H099", "H600", "H4010", "H-401", "H401 token=private")) {
            assertEquals(
                "Не удалось войти через Apple. Попробуйте ещё раз.",
                socialLoginError("Apple", "[ASI-$code]"),
            )
        }
    }
}
