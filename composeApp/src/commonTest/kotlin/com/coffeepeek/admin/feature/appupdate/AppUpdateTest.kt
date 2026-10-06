package com.coffeepeek.admin.feature.appupdate

import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.data.AppVersionResponse
import com.coffeepeek.admin.feature.appupdate.data.AppVersionDto
import com.coffeepeek.admin.feature.appupdate.data.toUpdate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUpdateTest {
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
    fun versionBoundariesAndRollback() {
        val policy = AppUpdate(140, 120, "https://example.com/app.apk")
        assertTrue(policy.isRequired(119))
        assertFalse(policy.isRequired(120))
        assertTrue(policy.isAvailable(120))
        assertFalse(policy.isAvailable(140))
        assertFalse(policy.isAvailable(150))
        assertFalse(AppUpdate(110, 0, policy.url).isAvailable(120))
        assertFalse(AppUpdate(140, 0, policy.url).isRequired(1))
        assertFalse(policy.shouldPrompt(120, 140))
        assertTrue(policy.shouldPrompt(120, 140, manual = true))
        assertTrue(policy.shouldPrompt(119, 140))
        assertTrue(policy.shouldPrompt(120, 130))
        assertFalse(policy.shouldPrompt(140, null, manual = true))
    }
}
