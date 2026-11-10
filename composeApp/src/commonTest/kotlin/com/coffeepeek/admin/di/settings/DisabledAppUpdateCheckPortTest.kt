package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.settings.domain.UpdateCheckOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DisabledAppUpdateCheckPortTest {
    @Test fun disabledCheckDoesNotClaimTheInstalledVersionIsUpToDate() = runTest {
        val environment = object : AppUpdateEnvironment {
            override val checksEnabled = false
            override val versionCode = 1L
            override val platform = "ios"
            override val channel: String? = null
        }
        val port = AppUpdateCheckPort(AppUpdateController(UnusedRepository, environment, backgroundScope))
        assertEquals(UpdateCheckOutcome.Disabled, port.check())
    }

    private object UnusedRepository : AppUpdateRepository {
        override suspend fun fetch(): AppUpdate? = error("No version request in disabled builds")
        override suspend fun dismissedVersion(): Long? = error("No settings access in disabled builds")
        override suspend fun dismiss(version: Long) = error("No settings writes in disabled builds")
    }
}
