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

/** The app-update row: [message] is the result text of a manual check, [updateAvailable] a newer version known to the app. */
data class UpdateCheckState(val checking: Boolean = false, val message: String? = null, val updateAvailable: Boolean = false)
