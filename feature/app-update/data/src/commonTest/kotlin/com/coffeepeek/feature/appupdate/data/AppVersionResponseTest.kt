package com.coffeepeek.feature.appupdate.data

import com.coffeepeek.feature.appupdate.domain.AppUpdate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppVersionResponseTest {
    @Test
    fun responseEnvelopeAndUntrustedPolicy() {
        val response = Json { ignoreUnknownKeys = true }.decodeFromString<AppVersionResponse>(
            """{"isSuccess":true,"message":"OK","data":{"versionCode":140,"minVersionCode":120,"url":"https://example.com/app.apk"},"statusCode":null}"""
        )
        assertEquals(AppUpdate(140, 120, "https://example.com/app.apk"), response.toUpdate())
        assertNull(response.copy(isSuccess = false).toUpdate())
        assertNull(response.copy(data = null).toUpdate())
        for (url in listOf("http://example.com/app.apk", "file:///app.apk", "javascript:alert(1)", "https://user:password@example.com/app.apk")) {
            assertNull(AppVersionResponse(true, AppVersionDto(140, 120, url)).toUpdate())
        }
        assertNull(AppVersionResponse(true, AppVersionDto(100, 120, "https://example.com")).toUpdate())
        assertNull(AppVersionResponse(true, AppVersionDto(100, -1, "https://example.com")).toUpdate())
    }

    @Test
    fun aPolicyWithoutAUsableUrlIsRejected() {
        for (bad in listOf("ftp://example.com/app.apk", "")) {
            assertNull(AppVersionResponse(true, AppVersionDto(140, 120, bad)).toUpdate(), bad)
        }
        assertEquals(AppUpdate(140, 140, "https://example.com/app.apk"), AppVersionResponse(true, AppVersionDto(140, 140, "https://example.com/app.apk")).toUpdate())
        // Characterized quirks of the legacy host check: Ktor parses these as having a host, so they are accepted.
        for (odd in listOf("https:///app.apk", "https://")) {
            assertEquals(odd, AppVersionResponse(true, AppVersionDto(140, 120, odd)).toUpdate()?.url, odd)
        }
    }
}
