@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.FakeSettingRepository
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferenceDiscoveryCityTest {
    private val minsk = Coordinates(53.900, 27.567)
    private val brest = Coordinates(52.109, 23.717)
    private val cities = listOf(CityEntry("minsk", "Минск", minsk), CityEntry("brest", "Брест", brest))

    @Test fun aPositionBeforeTheCatalogChoosesTheNearestCityOnTheFirstResolve() = runTest {
        val location = Location(brest)
        val city = PreferenceDiscoveryCity(CityPreference(FakeSettingRepository()), location, backgroundScope)
        assertEquals("brest", city.resolve(cities))
    }

    @Test fun aPositionAfterTheCatalogUpdatesTheDefaultUntilTheUserSelectsManually() = runTest {
        val location = Location(null)
        val preference = CityPreference(FakeSettingRepository())
        val city = PreferenceDiscoveryCity(preference, location, backgroundScope)
        assertEquals("minsk", city.resolve(cities))
        location.updateFromMap(brest)
        runCurrent()
        assertEquals("brest", city.selected.value?.id)
        preference.select("brest")
        location.updateFromMap(minsk)
        runCurrent()
        assertEquals("brest", city.selected.value?.id)
    }

    private class Location(point: Coordinates?) : DiscoveryLocation {
        override val current = MutableStateFlow(point)
        override fun refresh() = Unit
        override fun updateFromMap(point: Coordinates) { current.value = point }
    }
}
