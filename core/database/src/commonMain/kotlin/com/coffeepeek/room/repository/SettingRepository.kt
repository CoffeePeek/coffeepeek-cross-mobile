package com.coffeepeek.room.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.utils.JsonExt

interface SettingRepository {

    suspend fun save(model: Setting)

    suspend fun read(key: String): Setting?

    fun readFlow(key: String): Flow<Setting?>

    suspend fun readAll(): List<Setting>

    fun readAllFlow(): Flow<List<Setting>>

    suspend fun delete(key: String)

}

suspend inline fun <reified T> SettingRepository.saveSerializable(key: String, data: T?) {
    if (data == null) {
        delete(key)
        return
    }
    val encoded = JsonExt.json.encodeToString(data)
    save(Setting(key, encoded))
}

suspend inline fun <reified T> SettingRepository.readSerializable(key: String): T? {
    val data = read(key) ?: return null
    if (data.value.isBlank() || data.value == "null") return null
    return runCatching { JsonExt.json.decodeFromString<T>(data.value) }.getOrNull()
}

inline fun <reified T> SettingRepository.readSerializableFlow(key: String): Flow<T?> {
    return readFlow(key).map { setting ->
        val value = setting?.value ?: return@map null
        if (value.isBlank() || value == "null") return@map null
        runCatching { JsonExt.json.decodeFromString<T>(value) }.getOrNull()
    }
}