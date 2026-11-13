package com.coffeepeek.feature.settings.data.local

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository

/** Only unsynchronized choices are local. The server owns the settled account preference. */
internal class AnalyticsPreferencePending(private val settings: SettingRepository) {
    suspend fun migrate(sessions: SessionReader) {
        val legacy = settings.read(LEGACY)?.value ?: return
        val account = sessions.getSession()?.takeIf(sessions::isActiveSession)?.userId
        if (account != null && read(account) == null && legacy in setOf("accepted", "declined")) {
            settings.save(Setting(key(account), "legacy:${legacy == "accepted"}"))
        }
        settings.delete(LEGACY)
    }

    suspend fun read(accountId: String): String? = settings.read(key(accountId))?.value
    suspend fun write(accountId: String, enabled: Boolean) = settings.save(Setting(key(accountId), enabled.toString()))
    suspend fun clear(accountId: String, expected: String) {
        if (read(accountId) == expected) settings.delete(key(accountId))
    }

    private fun key(accountId: String) = "coffeepeek.analyticsPreference.pending.v1.$accountId"
    private companion object { const val LEGACY = "coffeepeek.analyticsConsent.v1" }
}
