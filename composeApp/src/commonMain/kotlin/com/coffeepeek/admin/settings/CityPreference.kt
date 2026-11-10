package com.coffeepeek.admin.settings

import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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

    suspend fun resolve(cityIds: List<String>): String? = try {
        val savedCityId = _selectedCityId.value ?: settingRepository.read(SELECTED_CITY_KEY)?.value
        val resolvedCityId = resolveSelectedCityId(savedCityId, cityIds)
        _selectedCityId.value = resolvedCityId
        // Persist the bootstrap choice only: a saved city that is temporarily absent from a
        // glitched catalog keeps its persisted value instead of being rewritten to the first entry.
        if (savedCityId == null && resolvedCityId != null) {
            settingRepository.save(Setting(SELECTED_CITY_KEY, resolvedCityId))
        }
        resolvedCityId
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }

    fun select(cityId: String) {
        if (cityId.isBlank() || cityId == _selectedCityId.value) return
        _selectedCityId.value = cityId
        scope.launch {
            try { settingRepository.save(Setting(SELECTED_CITY_KEY, cityId)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { }
        }
    }
}

internal fun resolveSelectedCityId(savedCityId: String?, cityIds: List<String>): String? =
    savedCityId?.takeIf { it in cityIds } ?: cityIds.firstOrNull()
