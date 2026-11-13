package com.coffeepeek.feature.settings.domain

/** Effective account setting; a server default is not proof of explicit consent. */
data class AnalyticsPreference(val enabled: Boolean)

interface AnalyticsPreferences {
    /** Bind the previous installation setting to the account restored at startup, once. */
    suspend fun prepare(): Result<Unit>
    suspend fun load(accountId: String): Result<AnalyticsPreference>
    suspend fun set(accountId: String, enabled: Boolean): Result<Unit>
}
