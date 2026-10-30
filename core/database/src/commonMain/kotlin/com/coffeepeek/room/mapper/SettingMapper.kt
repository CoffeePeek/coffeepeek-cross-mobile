package com.coffeepeek.room.mapper

import com.coffeepeek.room.entity.SettingEntity
import com.coffeepeek.room.model.Setting

object SettingMapper {


    fun Setting.toEntity() = SettingEntity(
        key = key,
        value = value
    )

    fun SettingEntity.toDTO() = Setting(
        key = key,
        value = value
    )

}