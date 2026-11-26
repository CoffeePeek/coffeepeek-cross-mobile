package com.coffeepeek.admin.theme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemeManager {
    private const val THEME_MODE_KEY = "theme_mode"

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val loaded = MutableStateFlow(false)
    val ready = loaded.asStateFlow()
    private var settingRepository: SettingRepository? = null
    private var isInitialized = false

    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun initialize(repository: SettingRepository) {
        if (isInitialized) return
        isInitialized = true
        settingRepository = repository
        observeTheme(repository)
    }
    private fun observeTheme(repository: SettingRepository) {
        scope.launch {
            repository.readFlow(THEME_MODE_KEY).catch {
                if (it is CancellationException) throw it
                emit(null)
            }.collect { setting ->
                val mode = ThemeMode.entries.firstOrNull { it.name == setting?.value } ?: ThemeMode.SYSTEM
                _themeMode.value = mode
                applyPlatformNightMode(mode)
                loaded.value = true
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        _themeMode.value = mode
        applyPlatformNightMode(mode)
        settingRepository?.let { repository ->
            scope.launch {
                repository.save(Setting(THEME_MODE_KEY, mode.name))
            }
        }
    }
}
