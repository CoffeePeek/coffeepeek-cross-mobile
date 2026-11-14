package com.coffeepeek.feature.settings.data

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.data.backend.AnalyticsPreferenceBackend
import com.coffeepeek.feature.settings.data.local.AnalyticsPreferencePending
import com.coffeepeek.feature.settings.data.repository.AccountAnalyticsPreferences
import com.coffeepeek.feature.settings.domain.AnalyticsPreferences
import com.coffeepeek.room.repository.SettingRepository
import io.ktor.client.HttpClient

fun createAnalyticsPreferences(client: HttpClient, sessions: SessionReader, settings: SettingRepository): AnalyticsPreferences =
    AccountAnalyticsPreferences(AnalyticsPreferenceBackend(client), AnalyticsPreferencePending(settings), sessions)
