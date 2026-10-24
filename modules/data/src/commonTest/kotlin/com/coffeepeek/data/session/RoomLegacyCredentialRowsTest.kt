package com.coffeepeek.data.session

import com.coffeepeek.core.session.SessionCredentials
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class RoomLegacyCredentialRowsTest {
    private class Settings(vararg initial: Pair<String, String>) : SettingRepository {
        val rows = initial.toMap().toMutableMap()
        override suspend fun save(model: Setting) { rows[model.key] = model.value }
        override suspend fun read(key: String): Setting? = rows[key]?.let { Setting(key, it) }
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll() = rows.map { Setting(it.key, it.value) }
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) { rows.remove(key) }
    }

    @Test fun readsTheStoredShapeAndToleratesMissingRefreshToken() = runTest {
        val rows = RoomLegacyCredentialRows(Settings(
            "SESSION" to """{"accessToken":"a","refreshToken":"r"}""",
            "API_KEYS" to """{"accessToken":"b"}""",
        ))
        assertEquals(SessionCredentials("a", "r"), rows.read("SESSION"))
        assertEquals(SessionCredentials("b", ""), rows.read("API_KEYS"))
    }

    @Test fun missingBlankNullOrUnreadableRowsAreNothing() = runTest {
        val rows = RoomLegacyCredentialRows(Settings("A" to "", "B" to "null", "C" to "not json"))
        for (key in listOf("A", "B", "C", "missing")) assertNull(rows.read(key), key)
    }

    @Test fun deleteRemovesTheRow() = runTest {
        val settings = Settings("SESSION" to """{"accessToken":"a"}""")
        RoomLegacyCredentialRows(settings).delete("SESSION")
        assertTrue(settings.rows.isEmpty())
    }
}
