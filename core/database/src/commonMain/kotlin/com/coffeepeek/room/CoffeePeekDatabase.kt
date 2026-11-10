@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.coffeepeek.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.coffeepeek.core.database.configureDatabaseDriver
import com.coffeepeek.room.dao.SettingDAO
import com.coffeepeek.room.entity.SettingEntity

@Database(
    version = 3,
    entities = [
        SettingEntity::class,
    ],
)
@ConstructedBy(CoffeePeekDatabaseConstructor::class)
abstract class CoffeePeekDatabase : RoomDatabase(), DB {

    abstract val settingDAO: SettingDAO

    companion object {

        fun RoomDatabase.Builder<CoffeePeekDatabase>.configure(): CoffeePeekDatabase {
            return configureDatabaseDriver()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }

    override fun clearAllTables() {}
}

interface DB {
    fun clearAllTables()
}

@Suppress("KotlinNoActualForExpect")
expect object CoffeePeekDatabaseConstructor : RoomDatabaseConstructor<CoffeePeekDatabase> {
    override fun initialize(): CoffeePeekDatabase
}
