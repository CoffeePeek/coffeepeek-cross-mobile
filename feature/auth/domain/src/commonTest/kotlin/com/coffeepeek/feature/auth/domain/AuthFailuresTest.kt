package com.coffeepeek.feature.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthFailuresTest {
    @Test fun credentialLikeMessagesAreWrongEmailOrPassword() {
        for (message in listOf(
            "Unauthorized", "Invalid credentials", "invalid login", "Incorrect password", "wrong password", "User not found",
            "Неверные данные", "Неправильный пароль", "Пользователь не найден",
        )) assertEquals(LoginFailure.WrongCredentials, classifyLoginFailure(message), message)
    }

    @Test fun networkLikeMessagesAreAConnectionProblem() {
        for (message in listOf(
            "timeout", "Request timed out", "Network is unreachable", "Failed to connect to /10.0.0.1", "Socket closed",
            "Unable to resolve host", "No internet", "Сеть недоступна", "Проблемы с соединением",
        )) assertEquals(LoginFailure.Network, classifyLoginFailure(message), message)
    }

    @Test fun credentialWordsWinAndEverythingElseIsUnknown() {
        assertEquals(LoginFailure.WrongCredentials, classifyLoginFailure("Unauthorized: connection reset"))
        for (message in listOf(null, "", "Unexpected server response", "Internal Server Error", "api.example")) {
            assertEquals(LoginFailure.Unknown, classifyLoginFailure(message), message)
        }
        // Characterized quirk: the Russian stem is "сеть", so "Нет сети" does not match.
        assertEquals(LoginFailure.Unknown, classifyLoginFailure("Нет сети"))
    }

    @Test fun socialFailuresAreToldByWhatTheyMean() {
        fun kind(message: String?) = classifySocialFailure(SocialProvider.Google, message).kind
        assertEquals(SocialFailureKind.AccountExists, kind("Account with this email already exists"))
        assertEquals(SocialFailureKind.AccountExists, kind("user already has an account"))
        assertEquals(SocialFailureKind.EmailNotVerified, kind("Google did not return a verified email"))
        assertEquals(SocialFailureKind.EmailNotVerified, kind("The email is not verified"))
        assertEquals(SocialFailureKind.Network, kind("Socket timeout"))
        assertEquals(SocialFailureKind.Generic, kind("something else"))
        assertEquals(SocialFailureKind.Generic, kind(null))
        assertEquals(SocialFailureKind.AccountExists, kind("account already exists, connection ok"), "Account words win over network words")
    }

    @Test fun googleDiagnosticsCarryOnlyTrustedPieces() {
        fun diagnostic(message: String) = classifySocialFailure(SocialProvider.Google, message).diagnostic
        assertEquals(SocialDiagnostic("GSI-E-4", null, null), diagnostic("Не удалось войти через Google. Попробуйте ещё раз. [GSI-E-4]"))
        assertEquals(SocialDiagnostic("GSI-E-61440", "com.google.GIDSignIn", null), diagnostic("private details [GSI-E-61440 D:com.google.GIDSignIn]"))
        assertEquals(
            SocialDiagnostic("GSI-E-61440", "org.openid.appauth.oauth_token", "invalid_rapt"),
            diagnostic("private details [GSI-E-61440 D:org.openid.appauth.oauth_token O:invalid_rapt]"),
        )
        assertNull(diagnostic("[GSI-E-61440 O:invalid_rapt;token=private]"))
        assertNull(diagnostic("private details [GSI-E-12345678901]"))
        assertNull(diagnostic("private details [GSI-E-61440 D:com.google;token=private]"))
        assertNull(diagnostic("no code at all"))
    }

    @Test fun appleDiagnosticsAreSeparateFromGoogleOnes() {
        fun diagnostic(message: String) = classifySocialFailure(SocialProvider.Apple, message).diagnostic
        assertNull(diagnostic("[GSI-T]"))
        assertEquals(
            SocialDiagnostic("ASI-E1000", "com.apple.AuthenticationServices.AuthorizationError", null),
            diagnostic("private-token=secret [ASI-E1000 D:com.apple.AuthenticationServices.AuthorizationError]"),
        )
        for (code in listOf("W", "C", "S", "T", "B", "CH", "X", "R", "A", "I", "P")) {
            assertEquals(SocialDiagnostic("ASI-$code", null, null), diagnostic("[ASI-$code]"), code)
        }
        assertEquals(SocialDiagnostic("ASI-H401", null, null), diagnostic("Сервер отклонил вход через Apple. [ASI-H401]"))
        assertNull(classifySocialFailure(SocialProvider.Google, "[ASI-X]").diagnostic)
    }
}
