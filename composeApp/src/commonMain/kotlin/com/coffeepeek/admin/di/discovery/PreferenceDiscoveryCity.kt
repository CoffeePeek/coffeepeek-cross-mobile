package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.rules.nearestCityId
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.module.Module

internal fun Module.registerDiscoveryCity(settings: SettingRepository) {
    single { CityPreference(settings) }
    single { AppDiscoveryLocation(get()) }
    single { PreferenceDiscoveryCity(get(), get<AppDiscoveryLocation>(), get()) }
}

internal class PreferenceDiscoveryCity(
    private val preference: CityPreference,
    private val location: DiscoveryLocation,
    scope: CoroutineScope,
) : DiscoveryCity {
    private val lock = Mutex()
    private var catalog = emptyList<CityEntry>()
    override val selected = preference.selected

    init {
        scope.launch {
            location.current.collect {
                lock.withLock {
                    if (catalog.isNotEmpty() && location.current.value?.isValid == true) resolveCurrentCity()
                }
            }
        }
    }

    override suspend fun resolve(cities: List<CityEntry>): String? = lock.withLock {
        catalog = cities
        resolveCurrentCity()
    }

    private suspend fun resolveCurrentCity(): String? =
        preference.resolve(catalog.map { it.id }, nearestCityId(catalog, location.current.value))
}
