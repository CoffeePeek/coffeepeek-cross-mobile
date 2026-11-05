package com.coffeepeek.room

import androidx.room.Room
import com.coffeepeek.room.CoffeePeekDatabase.Companion.configure

actual class MyRoomRepositoryImpl private constructor(
    database: CoffeePeekDatabase,
) : CoffeePeekDatabaseRepository(database) {

    companion object {
        private var repo: CoffeePeekDatabaseRepository? = null

        operator fun invoke(databasePath: String): DatabaseCore {
            if (repo == null) {
                repo = MyRoomRepositoryImpl(
                    Room.databaseBuilder<CoffeePeekDatabase>(
                        name = databasePath,
                        factory = { CoffeePeekDatabaseConstructor.initialize() },
                    ).configure(),
                )
            }
            return requireNotNull(repo)
        }
    }
}
