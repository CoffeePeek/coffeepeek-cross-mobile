package com.coffeepeek.admin.settings

import com.coffeepeek.feature.discovery.api.DiscoveryCitySelection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CityPreferenceTest {
    private val cities = listOf("minsk", "grodno")

    @Test
    fun keepsSavedCityWhenItStillExists() {
        assertEquals("grodno", resolveSelectedCityId("grodno", cities))
    }

    @Test
    fun selectsFirstCityWhenSavedCityIsMissing() {
        assertEquals("minsk", resolveSelectedCityId("removed", cities))
    }

    @Test
    fun returnsNullWhenCatalogHasNoCities() {
        assertNull(resolveSelectedCityId("minsk", emptyList()))
    }
}

class CityPreferenceStorageTest {
    private val cities = listOf("minsk", "grodno")

    @Test fun aSavedCityThatStillExistsIsKeptAndNotWrittenAgain() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "grodno"))
        val preference = CityPreference(settings)
        assertEquals("grodno", preference.resolve(cities))
        assertEquals(DiscoveryCitySelection("grodno", manual = false), preference.selected.value)
        assertEquals(emptyList(), settings.saves)
    }

    @Test fun aMissingOrRemovedCityFallsBackToTheFirstOneAndThatIsPersisted() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "removed"))
        assertEquals("minsk", CityPreference(settings).resolve(cities))
        assertEquals("minsk", settings.valueOf("selected_city_id"))

        val fresh = FakeSettingRepository()
        assertEquals("minsk", CityPreference(fresh).resolve(cities))
        assertEquals("minsk", fresh.valueOf("selected_city_id"))
    }

    @Test fun anEmptyCatalogResolvesToNothingAndSavesNothing() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "minsk"))
        val preference = CityPreference(settings)
        assertNull(preference.resolve(emptyList()))
        assertNull(preference.selected.value)
        assertEquals(emptyList(), settings.saves)
    }

    @Test fun anExplicitSelectionOfTheCurrentCityWinsOverLocationAfterRestart() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "minsk"))
        val preference = CityPreference(settings)
        preference.resolve(cities)
        preference.select("minsk")
        val restarted = CityPreference(settings)
        assertEquals("minsk", restarted.resolve(cities, nearbyCityId = "grodno"))
        assertEquals(DiscoveryCitySelection("minsk", manual = true), restarted.selected.value)
    }

    @Test fun locationCanReplaceAnExistingSelectionFromBeforeAutomaticDetection() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "minsk"))
        val preference = CityPreference(settings)
        assertEquals("grodno", preference.resolve(cities, nearbyCityId = "grodno"))
        assertEquals("grodno", settings.valueOf("selected_city_id"))
        assertEquals("grodno", CityPreference(settings).resolve(cities))
    }

    @Test fun selectIgnoresBlankAndRepeatedIds() = runTest {
        val settings = FakeSettingRepository()
        val preference = CityPreference(settings)
        preference.select("minsk")
        val before = settings.saves.size
        preference.select("")
        preference.select("  ".trim())
        preference.select("minsk")
        assertEquals(DiscoveryCitySelection("minsk", manual = true), preference.selected.value)
        assertEquals(before, settings.saves.size)
    }
}
