package com.coffeepeek.feature.settings.api

import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.domain.UpdateCheckOutcome
import com.coffeepeek.feature.settings.domain.UpdateCheckState
import com.coffeepeek.feature.settings.domain.UsageConsentState
import kotlinx.coroutines.flow.Flow

/** The app theme. The app owns the process-wide manager (startup, platform night mode, iOS shell); the screens only read and choose. */
interface ThemePort {
    fun current(): ThemeMode

    /** Replays the current mode, then every change. */
    fun observe(): Flow<ThemeMode>

    /** Takes effect immediately and is persisted by the app. */
    fun set(mode: ThemeMode)
}

/** The app owns persistence and applies this language to its Compose and native presentation. */
interface LanguagePort {
    /** The restored choice, or Russian when no choice has been saved. */
    fun current(): AppLanguage

    /** Replays the current choice, then every change. */
    fun observe(): Flow<AppLanguage>

    /** Persists first, then applies and publishes the choice. A failure preserves the previous language; cancellation is rethrown. */
    suspend fun set(language: AppLanguage): Result<Unit>
}

/** The default city for discovery. Feed and Map read the same selection and resolve it themselves. */
interface CityPort {
    /** Replays the current selection (`null` until it is resolved), then every change. */
    fun observeSelectedCityId(): Flow<String?>

    /**
     * Loads the catalog and, as production did, resolves the saved choice against it: a saved city that is gone falls back to the
     * first one and that is persisted. A failure changes nothing.
     */
    suspend fun loadCities(): Result<List<SettingsCity>>

    /** Ignores a blank or already selected id. The caller only offers ids from [loadCities]. */
    fun select(cityId: String)
}

/** Whether a signed-in account is active. Guests keep the tab but lose the signed-in rows. */
interface SettingsAccountPort {
    fun isSignedIn(): Boolean

    /** Replays the current value, then distinct changes. */
    fun observeSignedIn(): Flow<Boolean>
}

/** The analytics consent switch of the usage policy screen. */
interface UsageConsentPort {
    fun observe(): Flow<UsageConsentState>

    /** `false` also stops delivery and erases the queue (the app owns that). */
    suspend fun set(accepted: Boolean)
}

/** The app-update row. The update flow, prompt and installers stay app-owned. */
interface UpdateCheckPort {
    fun observe(): Flow<UpdateCheckState>

    /** A manual check; returns once it has finished, also when an automatic check was already running. */
    suspend fun check(): UpdateCheckOutcome
}

/** Actions that leave the app. The app owns the URLs and the share text. */
interface SettingsExternalActions {
    val canRateApp: Boolean
    fun openSupport()
    fun openTerms()
    fun openPrivacy()
    fun shareApp()
    fun rateApp()
}

/** Static facts about the build that the rows show. */
data class SettingsAppInfo(val versionName: String)
