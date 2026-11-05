package com.coffeepeek.room

import android.content.Context
import androidx.room.Room
import com.coffeepeek.room.CoffeePeekDatabase.Companion.configure

actual class MyRoomRepositoryImpl private constructor(
    database: CoffeePeekDatabase,
) : CoffeePeekDatabaseRepository(database) {

    companion object {

        private var repo: CoffeePeekDatabaseRepository? = null

        operator fun invoke(
            context: Context,
            databasePath: String,
        ): DatabaseCore {
            if (repo == null) synchronized(this) {
                if (repo == null) repo = MyRoomRepositoryImpl(
                    Room.databaseBuilder<CoffeePeekDatabase>(
                        context = context,
                        name = databasePath,
                    ).configure(),
                )
            }
            return repo!!
        }
    }
}
