package com.coffeepeek.room

import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.repository.SettingRepositoryImp

expect class MyRoomRepositoryImpl : CoffeePeekDatabaseRepository

abstract class CoffeePeekDatabaseRepository(
    database: CoffeePeekDatabase,
) : DatabaseCore {

    override val settingRepository: SettingRepository = SettingRepositoryImp(database.settingDAO)
}
