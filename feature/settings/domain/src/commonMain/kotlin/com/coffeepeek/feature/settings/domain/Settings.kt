package com.coffeepeek.feature.settings.domain

/** The app appearance choice. The names match the persisted values of the legacy theme setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** A city the user can pick as the default for discovery. */
data class SettingsCity(val id: String, val name: String)

/**
 * The analytics consent switch. [available] is true only for a signed-in user whose analytics identity is ready; the row is
 * not shown otherwise. [accepted] is false for both "declined" and "never asked".
 */
data class UsageConsentState(val available: Boolean, val accepted: Boolean) {
    companion object {
        val Unavailable = UsageConsentState(available = false, accepted = false)
    }
}

/** The app-update row: [checking] while any check runs, [updateAvailable] when the app knows a newer version. */
data class UpdateCheckState(val checking: Boolean = false, val updateAvailable: Boolean = false)

/** Result of a manual check. [Available] is answered by the app's own update prompt, not by Settings. */
enum class UpdateCheckOutcome { UpToDate, Available, Failed }
