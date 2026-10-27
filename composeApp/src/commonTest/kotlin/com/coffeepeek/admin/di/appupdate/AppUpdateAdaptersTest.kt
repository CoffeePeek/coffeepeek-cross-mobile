package com.coffeepeek.admin.di.appupdate

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUpdateAdaptersTest {
    private class Settings : SettingRepository {
        val rows = mutableMapOf<String, String>()
        override suspend fun save(model: Setting) { rows[model.key] = model.value }
        override suspend fun read(key: String): Setting? = rows[key]?.let { Setting(key, it) }
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll() = rows.map { Setting(it.key, it.value) }
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) { rows.remove(key) }
    }

    // versionCode is read from the application context, which a plain unit test does not have.
    @Test fun theEnvironmentIsTheBuildConfiguration() {
        assertEquals(AppConfig.updatePlatform, AppConfigUpdateEnvironment.platform)
        assertEquals(AppConfig.updateChannel, AppConfigUpdateEnvironment.channel)
    }

    @Test fun theDismissedVersionKeepsTheKeyOfEarlierReleases() = runBlocking<Unit> {
        val settings = Settings()
        val store = SettingsDismissedUpdateStore(settings)
        assertNull(store.read())
        store.write(140)
        assertEquals(mapOf("dismissed_app_version" to "140"), settings.rows)
        assertEquals(140L, store.read())
        for (unreadable in listOf("abc", "")) {
            settings.rows["dismissed_app_version"] = unreadable
            assertNull(store.read(), unreadable)
        }
    }
}
