package com.coffeepeek.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.coffeepeek.room.entity.SettingEntity

@Dao
interface SettingDAO {

    @Upsert
    suspend fun save(vararg entity: SettingEntity)

    @Query("SELECT * FROM setting_table WHERE `key` = :key")
    suspend fun read(key: String): SettingEntity?

    @Query("SELECT * FROM setting_table WHERE `key` = :key")
    fun readFlow(key: String): Flow<SettingEntity?>

    @Query("SELECT * FROM setting_table")
    suspend fun readAll(): List<SettingEntity>

    @Query("SELECT * FROM setting_table")
    fun readAllFlow(): Flow<List<SettingEntity>>

    @Query("DELETE FROM setting_table WHERE `key` = :key")
    suspend fun delete(key: String)

}