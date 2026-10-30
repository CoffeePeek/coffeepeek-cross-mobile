package com.coffeepeek.room.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.coffeepeek.room.dao.SettingDAO
import com.coffeepeek.room.mapper.SettingMapper.toDTO
import com.coffeepeek.room.mapper.SettingMapper.toEntity
import com.coffeepeek.room.model.Setting

class SettingRepositoryImp(
    private val dao: SettingDAO
) : SettingRepository {


    override suspend fun save(model: Setting) {
        dao.save(model.toEntity())
    }

    override suspend fun read(key: String): Setting? {
        return dao.read(key)?.toDTO()
    }

    override fun readFlow(key: String): Flow<Setting?> {
        return  dao.readFlow(key).map { it?.toDTO() }
    }

    override suspend fun readAll(): List<Setting> {
        return dao.readAll().map { it.toDTO() }
    }

    override fun readAllFlow(): Flow<List<Setting>> {
        return dao.readAllFlow().map { list -> list.map { it.toDTO() } }
    }

    override suspend fun delete(key: String) {
        return dao.delete(key)
    }

}