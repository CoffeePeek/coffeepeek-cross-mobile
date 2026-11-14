package com.coffeepeek.admin.settings

import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SELECTED_CITY_KEY = "selected_city_id"

class CityPreference(
    private val settingRepository: SettingRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _selectedCityId = MutableStateFlow<String?>(null)

    val selectedCityId: StateFlow<String?> = _selectedCityId.asStateFlow()

    suspend fun resolve(cityIds: List<String>): String? {
        val savedCityId = _selectedCityId.value ?: settingRepository.read(SELECTED_CITY_KEY)?.value
        val resolvedCityId = resolveSelectedCityId(savedCityId, cityIds)
        _selectedCityId.value = resolvedCityId

        if (resolvedCityId != null && resolvedCityId != savedCityId) {
            settingRepository.save(Setting(SELECTED_CITY_KEY, resolvedCityId))
        }
        return resolvedCityId
    }

    fun select(cityId: String) {
        if (cityId.isBlank() || cityId == _selectedCityId.value) return
        _selectedCityId.value = cityId
        scope.launch {
            settingRepository.save(Setting(SELECTED_CITY_KEY, cityId))
        }
    }
}

internal fun resolveSelectedCityId(savedCityId: String?, cityIds: List<String>): String? =
    savedCityId?.takeIf { it in cityIds } ?: cityIds.firstOrNull()
