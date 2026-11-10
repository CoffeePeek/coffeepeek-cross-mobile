package com.coffeepeek.admin.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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

/** Parity tests for the stateful half of the existing `CityPreference` (settings migration step 1). */
class CityPreferenceStorageTest {
    private val cities = listOf("minsk", "grodno")
    private suspend fun await(condition: () -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition()) delay(10) } }

    @Test fun aSavedCityThatStillExistsIsKeptAndNotWrittenAgain() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "grodno"))
        val preference = CityPreference(settings)
        assertEquals("grodno", preference.resolve(cities))
        assertEquals("grodno", preference.selectedCityId.value)
        assertEquals(emptyList(), settings.saves)
    }

    @Test fun aMissingOrRemovedCityFallsBackInMemoryWithoutRewritingTheSavedChoice() = runTest {
        // A transient catalog without the saved city must not rewrite the persisted choice.
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "removed"))
        val preference = CityPreference(settings)
        assertEquals("minsk", preference.resolve(cities))
        assertEquals("minsk", preference.selectedCityId.value)
        assertEquals("removed", settings.valueOf("selected_city_id"))

        val fresh = FakeSettingRepository()
        assertEquals("minsk", CityPreference(fresh).resolve(cities))
        assertEquals("minsk", fresh.valueOf("selected_city_id"))
    }

    @Test fun anEmptyCatalogResolvesToNothingAndSavesNothing() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "minsk"))
        val preference = CityPreference(settings)
        assertNull(preference.resolve(emptyList()))
        assertNull(preference.selectedCityId.value)
        assertEquals(emptyList(), settings.saves)
    }

    @Test fun theInMemoryChoiceWinsOverTheStoredOneOnTheNextResolve() = runTest {
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "minsk"))
        val preference = CityPreference(settings)
        preference.resolve(cities)
        preference.select("grodno")
        assertEquals("grodno", preference.selectedCityId.value)
        await { settings.valueOf("selected_city_id") == "grodno" }
        assertEquals("grodno", preference.resolve(cities))
    }

    @Test fun selectIgnoresBlankAndRepeatedIds() = runTest {
        val settings = FakeSettingRepository()
        val preference = CityPreference(settings)
        preference.resolve(cities)
        val before = settings.saves.size
        preference.select("")
        preference.select("  ".trim())
        preference.select("minsk")
        delay(150)
        assertEquals("minsk", preference.selectedCityId.value)
        assertEquals(before, settings.saves.size)
    }
}
