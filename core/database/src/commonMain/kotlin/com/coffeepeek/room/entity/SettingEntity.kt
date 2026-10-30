package com.coffeepeek.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setting_table")
data class SettingEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo("key")
    val key: String,
    @ColumnInfo("value")
    val value: String
)
