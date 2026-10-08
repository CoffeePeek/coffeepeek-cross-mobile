package com.coffeepeek.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.repository.readSerializable
import com.coffeepeek.room.utils.JsonExt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AndroidEncryptedSessionStore(
    context: Context,
    private val legacySettings: com.coffeepeek.room.repository.SettingRepository,
) : SessionSecureStore {

    private val prefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val state = MutableStateFlow(readFromPrefs())

    override suspend fun read(): AuthResp? {
        migrateFromRoomIfNeeded()
        return state.value ?: readFromPrefs().also { state.value = it }
    }

    override suspend fun write(data: AuthResp?) {
        migrateFromRoomIfNeeded()
        persist(data)
        legacySettings.delete(RoomSessionSecureStore.SESSION_KEY)
    }

    private fun persist(data: AuthResp?) {
        val editor = prefs.edit()
        if (data == null) {
            editor.remove(PREFS_KEY)
        } else {
            editor.putString(PREFS_KEY, JsonExt.json.encodeToString(data))
        }
        editor.apply()
        state.value = data
    }

    override fun observe(): Flow<AuthResp?> = state.asStateFlow()

    private fun readFromPrefs(): AuthResp? {
        val raw = prefs.getString(PREFS_KEY, null) ?: return null
        if (raw.isBlank()) return null
        return runCatching { JsonExt.json.decodeFromString<AuthResp>(raw) }.getOrNull()
    }

    private suspend fun migrateFromRoomIfNeeded() {
        if (readFromPrefs() != null) return
        val legacy = legacySettings.readSerializable<AuthResp>(RoomSessionSecureStore.SESSION_KEY)
            ?: legacySettings.readSerializable<AuthResp>(LEGACY_SESSION_KEY)
            ?: return
        persist(legacy)
        legacySettings.delete(RoomSessionSecureStore.SESSION_KEY)
        legacySettings.delete(LEGACY_SESSION_KEY)
    }

    companion object {
        private const val PREFS_NAME = "cp_secure_session"
        private const val PREFS_KEY = "session"
        private const val LEGACY_SESSION_KEY = "API_KEYS"
    }
}

internal actual fun createPlatformSessionSecureStore(
    database: DatabaseCore,
    platformContext: Any?,
): SessionSecureStore {
    val context = platformContext as? Context
        ?: error("Android Context is required for secure session storage")
    return AndroidEncryptedSessionStore(
        context = context.applicationContext,
        legacySettings = database.settingRepository,
    )
}
