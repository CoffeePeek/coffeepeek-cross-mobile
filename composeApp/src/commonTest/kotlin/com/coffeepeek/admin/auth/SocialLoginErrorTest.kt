package com.coffeepeek.admin.auth

import kotlin.test.Test
import kotlin.test.assertEquals

internal val russianSocialErrorCopy = SocialLoginErrorCopy(
    "Аккаунт с этим email уже существует. Используйте способ входа, с которым вы регистрировались.",
    "%1\$s не передал подтверждённый email. Разрешите доступ к email и попробуйте ещё раз.",
    "Не удалось подключиться. Проверьте интернет-соединение",
    "Не удалось войти через %1\$s. Попробуйте ещё раз.", "код", "домен", "OAuth",
)

class SocialLoginErrorTest {
    private val belarusianCopy = SocialLoginErrorCopy(
        "Уліковы запіс з гэтым email ужо існуе.", "%1\$s не перадаў пацверджаны email.",
        "Не ўдалося падключыцца. Праверце інтэрнэт-злучэнне",
        "Не ўдалося ўвайсці праз %1\$s. Паспрабуйце яшчэ раз.", "код", "дамен", "OAuth",
    )

    private fun socialLoginError(provider: String, message: String?) =
        com.coffeepeek.admin.auth.socialLoginError(provider, message, russianSocialErrorCopy)

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

    @Test
    fun belarusianCopyKeepsMachineDiagnosticsWithoutDisplayingTheProviderText() {
        assertEquals(
            "Не ўдалося ўвайсці праз Apple. Паспрабуйце яшчэ раз. (код ASI-E1000; дамен com.apple.AuthenticationServices.AuthorizationError)",
            com.coffeepeek.admin.auth.socialLoginError("Apple",
                "Не удалось войти через Apple [ASI-E1000 D:com.apple.AuthenticationServices.AuthorizationError] private-token=secret", belarusianCopy),
        )
    }

    @Test
    fun belarusianClassificationUsesTheSameAccountAndNetworkRules() {
        assertEquals("Уліковы запіс з гэтым email ужо існуе.",
            com.coffeepeek.admin.auth.socialLoginError("Google", "email account already exists", belarusianCopy))
        assertEquals("Не ўдалося падключыцца. Праверце інтэрнэт-злучэнне (код ASI-X)",
            com.coffeepeek.admin.auth.socialLoginError("Apple", "network unavailable [ASI-X]", belarusianCopy))
        assertEquals("Apple не перадаў пацверджаны email.",
            com.coffeepeek.admin.auth.socialLoginError("Apple", "email is not verified", belarusianCopy))
    }
}
