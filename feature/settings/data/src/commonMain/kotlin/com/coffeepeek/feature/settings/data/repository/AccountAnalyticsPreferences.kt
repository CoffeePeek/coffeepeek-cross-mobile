package com.coffeepeek.feature.settings.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.data.backend.AnalyticsPreferenceBackend
import com.coffeepeek.feature.settings.data.local.AnalyticsPreferencePending
import com.coffeepeek.feature.settings.domain.AnalyticsPreference
import com.coffeepeek.feature.settings.domain.AnalyticsPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AccountAnalyticsPreferences(
    private val backend: AnalyticsPreferenceBackend,
    private val pending: AnalyticsPreferencePending,
    private val sessions: SessionReader,
) : AnalyticsPreferences {
    private val mutex = Mutex()
    private var prepared = false

    override suspend fun prepare() = requestResult {
        mutex.withLock {
            if (!prepared) {
                pending.migrate(sessions)
                prepared = true
            }
        }
    }

    override suspend fun load(accountId: String) = requestResult {
        mutex.withLock {
            val generation = requireAccount(accountId)
            val choice = pending.read(accountId)
            val enabled = if (choice == null) backend.load(accountId) else synchronize(accountId, choice)
            requireAccount(accountId, generation)
            AnalyticsPreference(enabled)
        }
    }

    override suspend fun set(accountId: String, enabled: Boolean) = requestResult {
        mutex.withLock {
            val generation = requireAccount(accountId)
            pending.write(accountId, enabled)
            requireAccount(accountId, generation)
            synchronize(accountId, enabled.toString())
            requireAccount(accountId, generation)
            Unit
        }
    }

    private suspend fun synchronize(accountId: String, choice: String): Boolean {
        val enabled = choice.removePrefix("legacy:").toBooleanStrict()
        val saved = backend.set(accountId, enabled, onlyIfUnset = choice.startsWith("legacy:"))
        pending.clear(accountId, choice)
        return saved
    }

    private fun requireAccount(accountId: String, generation: Long = sessions.accountGeneration()): Long {
        val session = sessions.peekSession()
        check(sessions.isActiveSession(session) && session?.userId == accountId && sessions.accountGeneration() == generation) {
            "Analytics preference session changed"
        }
        return generation
    }
}
