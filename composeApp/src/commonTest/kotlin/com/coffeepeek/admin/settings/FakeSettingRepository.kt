package com.coffeepeek.admin.settings

import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory settings with a save log; safe to read from the real dispatchers the settings classes use. */
internal class FakeSettingRepository(initial: Map<String, String> = emptyMap()) : SettingRepository {
    private val values = MutableStateFlow(initial)
    private val log = MutableStateFlow<List<Setting>>(emptyList())
    val saves: List<Setting> get() = log.value
    fun put(key: String, value: String) = values.update { it + (key to value) }
    fun valueOf(key: String): String? = values.value[key]
    override suspend fun save(model: Setting) { log.update { it + model }; put(model.key, model.value) }
    override suspend fun read(key: String): Setting? = values.value[key]?.let { Setting(key, it) }
    override fun readFlow(key: String): Flow<Setting?> = values.map { map -> map[key]?.let { Setting(key, it) } }
    override suspend fun readAll(): List<Setting> = values.value.map { Setting(it.key, it.value) }
    override fun readAllFlow(): Flow<List<Setting>> = values.map { map -> map.map { Setting(it.key, it.value) } }
    override suspend fun delete(key: String) { values.update { it - key } }
}
