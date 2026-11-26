package com.coffeepeek.core.session

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Characterizes what "active session" and the JWT helpers mean before they move to core/session. */
@OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class)
class SessionAuthTest {
    private fun now() = Clock.System.now().epochSeconds
    private fun jwt(payload: String) = "header." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        .encode(payload.encodeToByteArray()) + ".signature"
    private fun token(exp: Long?, sub: String? = "user") = jwt(
        listOfNotNull(sub?.let { "\"sub\":\"$it\"" }, exp?.let { "\"exp\":$it" }).joinToString(",", "{", "}"),
    )

    @Test fun missingSessionIsNotActive() = assertFalse(SessionAuth.isActive(null))

    @Test fun blankTokenOrUserIdIsNotActive() {
        assertFalse(SessionAuth.isActive(Session(" ", "refresh", "user")))
        assertFalse(SessionAuth.isActive(Session(token(now() + 3600), "refresh", null)))
        assertFalse(SessionAuth.isActive(Session(token(now() + 3600), "refresh", " ")))
    }

    @Test fun unexpiredTokenIsActiveWithoutRefreshToken() {
        assertTrue(SessionAuth.isActive(Session(token(now() + 3600), null, "user")))
        assertTrue(SessionAuth.isActive(Session(token(now() + 3600), "", "user")))
    }

    @Test fun expiredTokenIsActiveOnlyWithARefreshToken() {
        val expired = token(now() - 3600)
        assertFalse(SessionAuth.isActive(Session(expired, null, "user")))
        assertFalse(SessionAuth.isActive(Session(expired, " ", "user")))
        assertTrue(SessionAuth.isActive(Session(expired, "refresh", "user")))
    }

    @Test fun tokenWithoutReadableExpiryCountsAsUnexpired() {
        assertTrue(SessionAuth.isActive(Session("opaque-token", null, "user")))
        assertTrue(SessionAuth.isActive(Session(token(exp = null), null, "user")))
    }

    @Test fun leewayTreatsAlmostExpiredTokenAsExpired() {
        assertTrue(JwtUtils.isAccessTokenExpired(token(now() + 10)))
        assertFalse(JwtUtils.isAccessTokenExpired(token(now() + 300)))
        assertFalse(JwtUtils.isAccessTokenExpired(token(now() + 10), leewaySeconds = 0))
        assertTrue(SessionAuth.isActive(Session(token(now() + 10), "refresh", "user")))
        assertFalse(SessionAuth.isActive(Session(token(now() + 10), null, "user")))
    }

    @Test fun extractsSubjectAndExpiry() {
        val value = token(1_900_000_000L, sub = "abc-123")
        assertEquals("abc-123", JwtUtils.extractUserId(value))
        assertEquals(1_900_000_000L, JwtUtils.extractExp(value))
    }

    @Test fun malformedTokensYieldNothing() {
        for (bad in listOf("", "opaque", "a.b", "a.%%%.c", jwt("not json"), jwt("{}"))) {
            assertNull(JwtUtils.extractUserId(bad), bad)
            assertNull(JwtUtils.extractExp(bad), bad)
            assertFalse(JwtUtils.isAccessTokenExpired(bad), bad)
        }
    }

    @Test fun serverAccessExpiryOverridesAContradictoryJwtExpiry() {
        val current = Instant.parse("2026-11-25T12:00:00Z")
        val expiredJwt = Session(token(current.epochSeconds - 3600), null, "user", current + 3600.seconds)
        val futureJwt = Session(token(current.epochSeconds + 3600), null, "user", current - 1.seconds)
        assertTrue(SessionAuth.isActive(expiredJwt, current))
        assertFalse(SessionAuth.isActive(futureJwt, current))
    }

    @Test fun serverAccessExpiryAppliesToOpaqueTokensAndKeepsTheRefreshLeeway() {
        val current = Instant.parse("2026-11-25T12:00:00Z")
        val session = Session("opaque", null, "user", current + 31.seconds)
        assertTrue(SessionAuth.isActive(session, current))
        assertFalse(SessionAuth.isActive(session.copy(accessTokenExpiresAt = current + 30.seconds), current))
        assertFalse(SessionAuth.isActive(session.copy(accessTokenExpiresAt = current - 1.seconds), current))
    }

    @Test fun legacySessionsStillUseJwtExpiryWhenServerExpiryIsAbsent() {
        val current = Instant.parse("2026-11-25T12:00:00Z")
        assertTrue(SessionAuth.isActive(Session(token(current.epochSeconds + 31), null, "user"), current))
        assertFalse(SessionAuth.isActive(Session(token(current.epochSeconds + 30), null, "user"), current))
        assertTrue(SessionAuth.isActive(Session("opaque", null, "user"), current))
    }

    @Test fun refreshCredentialsRemainRecoverableDespitePastExpiriesOrClockSkew() {
        val current = Instant.parse("2026-11-25T12:00:00Z")
        val session = Session("opaque", "refresh", "user", current - 3600.seconds, current - 1.seconds)
        assertTrue(SessionAuth.isActive(session, current))
        assertTrue(SessionAuth.isActive(session, Instant.parse("2099-01-01T00:00:00Z")))
        assertFalse(SessionAuth.isActive(session.copy(refreshToken = " "), current))
    }

    @Test fun expiryMetadataCannotMakeMissingCredentialsActive() {
        val future = Instant.parse("2099-01-01T00:00:00Z")
        assertFalse(SessionAuth.isActive(Session("", "refresh", "user", future, future)))
        assertFalse(SessionAuth.isActive(Session("opaque", "refresh", null, future, future)))
    }
}
