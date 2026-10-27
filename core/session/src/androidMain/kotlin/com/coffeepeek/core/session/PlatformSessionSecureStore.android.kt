package com.coffeepeek.core.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AndroidEncryptedSessionStore(
    context: Context,
    private val legacyRows: LegacyCredentialRows,
) : SessionSecureStore {

    private val prefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val state = MutableStateFlow(readFromPrefs())

    override suspend fun read(): SessionCredentials? {
        migrateFromRoomIfNeeded()
        return state.value ?: readFromPrefs().also { state.value = it }
    }

    override suspend fun write(data: SessionCredentials?) {
        migrateFromRoomIfNeeded()
        persist(data)
        legacyRows.delete(LegacyCredentialRows.SESSION)
    }

    private fun persist(data: SessionCredentials?) {
        val editor = prefs.edit()
        if (data == null) {
            editor.remove(PREFS_KEY)
        } else {
            editor.putString(PREFS_KEY, SessionJson.encodeToString(data))
        }
        editor.apply()
        state.value = data
    }

    override fun observe(): Flow<SessionCredentials?> = state.asStateFlow()

    private fun readFromPrefs(): SessionCredentials? {
        val raw = prefs.getString(PREFS_KEY, null) ?: return null
        if (raw.isBlank()) return null
        return runCatching { SessionJson.decodeFromString<SessionCredentials>(raw) }.getOrNull()
    }

    private suspend fun migrateFromRoomIfNeeded() {
        if (readFromPrefs() != null) return
        val legacy = legacyRows.read(LegacyCredentialRows.SESSION)
            ?: legacyRows.read(LegacyCredentialRows.API_KEYS)
            ?: return
        persist(legacy)
        legacyRows.delete(LegacyCredentialRows.SESSION)
        legacyRows.delete(LegacyCredentialRows.API_KEYS)
    }

    companion object {
        private const val PREFS_NAME = "cp_secure_session"
        private const val PREFS_KEY = "session"
    }
}

actual fun createPlatformSessionSecureStore(
    platformContext: Any?,
    legacyRows: LegacyCredentialRows,
): SessionSecureStore {
    val context = platformContext as? Context
        ?: error("Android Context is required for secure session storage")
    return AndroidEncryptedSessionStore(
        context = context.applicationContext,
        legacyRows = legacyRows,
    )
}
