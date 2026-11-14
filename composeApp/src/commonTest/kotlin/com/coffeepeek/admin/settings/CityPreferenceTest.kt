package com.coffeepeek.admin.settings

import com.coffeepeek.feature.discovery.api.DiscoveryCitySelection
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test fun aMissingOrRemovedCityFallsBackInMemoryWithoutRewritingTheSavedChoice() = runTest {
        // A transient catalog without the saved city must not rewrite the persisted choice.
        val settings = FakeSettingRepository(mapOf("selected_city_id" to "removed"))
        val preference = CityPreference(settings)
        assertEquals("minsk", preference.resolve(cities))
        assertEquals(DiscoveryCitySelection("minsk", manual = false), preference.selected.value)
        assertEquals("removed", settings.valueOf("selected_city_id"))

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

    @Test fun missingManualCityReturnsWhenTheFullCatalogIsAvailableAgain() = runTest {
        val settings = FakeSettingRepository(mapOf("manual_city_id" to "grodno", "selected_city_id" to "grodno"))
        val preference = CityPreference(settings)
        assertEquals("minsk", preference.resolve(listOf("minsk"), nearbyCityId = "minsk"))
        assertEquals(emptyList(), settings.saves)
        assertEquals("grodno", preference.resolve(cities, nearbyCityId = "minsk"))
        assertEquals(DiscoveryCitySelection("grodno", manual = true), preference.selected.value)
    }

    @Test fun aFailedManualWriteKeepsTheChoiceInMemoryAndCanBeRetried() = runTest {
        val settings = FakeSettingRepository()
        var diskUnavailable = true
        val storage = object : SettingRepository by settings {
            override suspend fun save(model: Setting) {
                check(!diskUnavailable)
                settings.save(model)
            }
        }
        val preference = CityPreference(storage)
        preference.select("grodno")
        assertEquals("grodno", preference.resolve(cities, nearbyCityId = "minsk"))
        assertNull(settings.valueOf("manual_city_id"))
        diskUnavailable = false
        preference.select("grodno")
        assertEquals("grodno", CityPreference(settings).resolve(cities, nearbyCityId = "minsk"))
    }

    @Test fun aFailedReadDoesNotOverwriteStorageOrPreventDetection() = runTest {
        val settings = FakeSettingRepository()
        val storage = object : SettingRepository by settings {
            override suspend fun read(key: String): Setting? = error("Unavailable")
        }
        val preference = CityPreference(storage)
        assertEquals("grodno", preference.resolve(cities, nearbyCityId = "grodno"))
        assertEquals("grodno", preference.resolve(cities))
        assertEquals(emptyList(), settings.saves)
    }

    @Test fun aCancelledReadIsPropagated() = runTest {
        val settings = FakeSettingRepository()
        val storage = object : SettingRepository by settings {
            override suspend fun read(key: String): Setting? = throw CancellationException()
        }
        assertFailsWith<CancellationException> { CityPreference(storage).resolve(cities) }
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
