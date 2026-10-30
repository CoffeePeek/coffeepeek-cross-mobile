package com.coffeepeek.room

import com.coffeepeek.room.repository.SettingRepository

object Database

interface DatabaseCore {
    val settingRepository: SettingRepository
}
