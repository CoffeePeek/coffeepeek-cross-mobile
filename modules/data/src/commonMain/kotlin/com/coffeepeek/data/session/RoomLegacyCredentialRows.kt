package com.coffeepeek.data.session

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.core.session.LegacyCredentialRows
import com.coffeepeek.core.session.SessionCredentials
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.repository.readSerializable

/** Reads the pre-secure-store session rows kept in the legacy Room settings table. */
class RoomLegacyCredentialRows(private val settings: SettingRepository) : LegacyCredentialRows {
    override suspend fun read(key: String): SessionCredentials? =
        settings.readSerializable<AuthResp>(key)?.let { SessionCredentials(it.accessToken, it.refreshToken) }

    override suspend fun delete(key: String) = settings.delete(key)
}
