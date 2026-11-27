package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.di.attribution.AppAttribution
import kotlin.test.*

class AttributionAppLinkTest {
    @Test fun webPasswordRecoveryAndExternalDomainsAreNotConsumedByTheApp() {
        AppAttribution.pending.value = null
        assertFalse(AppAttribution.open("https://coffeepeek.by/reset-password?token=private"))
        assertFalse(AppAttribution.open("https://coffeepeek.by.attacker.example/l/abcdefghijklmnop"))
        assertNull(AppAttribution.pending.value)
    }
}
