package com.coffeepeek.admin.session

import com.coffeepeek.core.session.LegacyCredentialRows
import com.coffeepeek.core.session.SessionCredentials
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.repository.readSerializable
import kotlinx.serialization.Serializable

/** Reads the pre-secure-store session rows kept in the legacy Room settings table. */
class RoomLegacyCredentialRows(private val settings: SettingRepository) : LegacyCredentialRows {
    override suspend fun read(key: String): SessionCredentials? =
        settings.readSerializable<LegacyAuthRow>(key)?.let { SessionCredentials(it.accessToken, it.refreshToken) }

    override suspend fun delete(key: String) = settings.delete(key)
}

/** The JSON an old build stored under a session key. */
@Serializable
internal data class LegacyAuthRow(val accessToken: String, val refreshToken: String = "")
