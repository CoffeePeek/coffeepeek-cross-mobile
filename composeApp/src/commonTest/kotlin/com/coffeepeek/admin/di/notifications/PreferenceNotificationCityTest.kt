package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.FakeSettingRepository
import com.coffeepeek.feature.notifications.api.NotificationCitySelection
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreferenceNotificationCityTest {
    @Test fun startupDoesNotEmitNullAndTheStoredAutomaticSelectionIsReplayedAfterResolution() = runTest {
        val preference = CityPreference(FakeSettingRepository(mapOf("selected_city_id" to "brest")))
        val port = PreferenceNotificationCity(preference)
        val received = mutableListOf<NotificationCitySelection>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { port.observeSelectedCity().toList(received) }
        runCurrent()
        assertTrue(received.isEmpty())
        preference.resolve(listOf("minsk", "brest"))
        runCurrent()
        assertEquals(listOf(NotificationCitySelection("brest")), received)
        assertEquals(NotificationCitySelection("brest"), port.observeSelectedCity().first())
    }

    @Test fun existingAutomaticAndManualSelectionRulesDriveTheSamePassivePort() = runTest {
        val preference = CityPreference(FakeSettingRepository())
        val received = mutableListOf<NotificationCitySelection>()
        val port = PreferenceNotificationCity(preference)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { port.observeSelectedCity().toList(received) }
        preference.resolve(listOf("minsk", "brest"), nearbyCityId = "brest")
        preference.select("minsk")
        preference.resolve(listOf("minsk", "brest"), nearbyCityId = "brest")
        runCurrent()
        assertEquals(listOf(NotificationCitySelection("brest"), NotificationCitySelection("minsk")), received)
        assertEquals("minsk", preference.selected.value?.id)
    }

    @Test fun onlyAnActualEmptyResolutionProducesAnExplicitClear() = runTest {
        val preference = CityPreference(FakeSettingRepository())
        val port = PreferenceNotificationCity(preference)
        preference.resolve(emptyList())
        assertEquals(NotificationCitySelection(null), port.observeSelectedCity().first())
    }
}
