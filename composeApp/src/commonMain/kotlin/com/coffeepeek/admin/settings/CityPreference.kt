package com.coffeepeek.admin.settings

import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.feature.discovery.api.DiscoveryCitySelection
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
    val selected = selection.asStateFlow()

    suspend fun resolve(cityIds: List<String>, nearbyCityId: String? = null): String? = lock.withLock {
        val manualCityId = settingRepository.read(MANUAL_CITY_KEY)?.value
        if (manualCityId != null && manualCityId in cityIds) {
            selection.value = DiscoveryCitySelection(manualCityId, manual = true)
            return@withLock manualCityId
        }
        val savedCityId = settingRepository.read(SELECTED_CITY_KEY)?.value
        val resolvedCityId = nearbyCityId?.takeIf { it in cityIds } ?: resolveSelectedCityId(savedCityId, cityIds)
        if (resolvedCityId != null && resolvedCityId != savedCityId) {
            settingRepository.save(Setting(SELECTED_CITY_KEY, resolvedCityId))
        }
        selection.value = resolvedCityId?.let { DiscoveryCitySelection(it, manual = false) }
        resolvedCityId
    }

    suspend fun select(cityId: String) = lock.withLock {
        val manual = DiscoveryCitySelection(cityId, manual = true)
        if (cityId.isBlank() || manual == selection.value) return@withLock
        settingRepository.save(Setting(MANUAL_CITY_KEY, cityId))
        selection.value = manual
    }
}

internal fun resolveSelectedCityId(savedCityId: String?, cityIds: List<String>): String? =
    savedCityId?.takeIf { it in cityIds } ?: cityIds.firstOrNull()
