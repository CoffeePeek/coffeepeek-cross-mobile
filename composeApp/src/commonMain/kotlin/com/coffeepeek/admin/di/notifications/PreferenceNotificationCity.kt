package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.feature.notifications.api.NotificationCity
import com.coffeepeek.feature.notifications.api.NotificationCitySelection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Adapts the same current city that Settings/Discovery use, without requesting location or loading catalogs. */
internal class PreferenceNotificationCity(private val preference: CityPreference) : NotificationCity {
    override fun observeSelectedCity() = preference.resolvedSelection
        .map { NotificationCitySelection(it?.id) }
        .distinctUntilChanged()
}
