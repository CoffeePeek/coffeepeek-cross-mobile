package com.coffeepeek.admin.settings

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.discovery.api.DiscoveryCitySelection
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val SELECTED_CITY_KEY = "selected_city_id"
private const val MANUAL_CITY_KEY = "manual_city_id"

internal class CityPreference(
    private val settingRepository: SettingRepository,
) {
    private val lock = Mutex()
    private val selection = MutableStateFlow<DiscoveryCitySelection?>(null)
    private var manualCityId: String? = null
    private var manualSavePending = false
    private var detectedCityId: String? = null
    val selected = selection.asStateFlow()

    suspend fun resolve(cityIds: List<String>, nearbyCityId: String? = null): String? = lock.withLock {
        val manual = manualCityId ?: readCityId(MANUAL_CITY_KEY).getOrNull()
        manualCityId = manual
        if (manual != null && manual in cityIds) {
            selection.value = DiscoveryCitySelection(manual, manual = true)
            return@withLock manual
        }
        val resolved = resolveAutomaticCity(cityIds, nearbyCityId)
        selection.value = resolved?.let { DiscoveryCitySelection(it, manual = false) }
        resolved
    }

    private suspend fun resolveAutomaticCity(cityIds: List<String>, nearbyCityId: String?): String? {
        val stored = readCityId(SELECTED_CITY_KEY)
        val saved = stored.getOrNull()
        val detected = nearbyCityId?.takeIf { it in cityIds } ?: detectedCityId?.takeIf { it in cityIds }
        val previous = selection.value?.takeUnless { it.manual }?.id
        val resolved = detected ?: resolveSelectedCityId(stored.getOrElse { previous }, cityIds)
        // A catalog temporarily missing the saved city must not erase it.
        val shouldSave = saved == null || (detected != null && saved in cityIds)
        if (stored.isSuccess && shouldSave && resolved != null && resolved != saved) {
            saveCityId(SELECTED_CITY_KEY, resolved)
        }
        if (detected != null) detectedCityId = detected
        return resolved
    }

    suspend fun select(cityId: String) = lock.withLock {
        if (cityId.isBlank() || (cityId == manualCityId && !manualSavePending)) return@withLock
        manualSavePending = saveCityId(MANUAL_CITY_KEY, cityId).isFailure
        manualCityId = cityId
        selection.value = DiscoveryCitySelection(cityId, manual = true)
    }

    private suspend fun readCityId(key: String) = requestResult { settingRepository.read(key)?.value }

    private suspend fun saveCityId(key: String, cityId: String) = requestResult {
        settingRepository.save(Setting(key, cityId))
    }
}

internal fun resolveSelectedCityId(savedCityId: String?, cityIds: List<String>): String? =
    savedCityId?.takeIf { it in cityIds } ?: cityIds.firstOrNull()
