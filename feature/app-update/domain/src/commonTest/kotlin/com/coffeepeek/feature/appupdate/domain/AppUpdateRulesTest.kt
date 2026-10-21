package com.coffeepeek.feature.appupdate.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateRulesTest {
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
