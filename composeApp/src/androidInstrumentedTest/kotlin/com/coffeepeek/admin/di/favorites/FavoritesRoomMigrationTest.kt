package com.coffeepeek.admin.di.favorites

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.domain.repository.FavoriteRepository as LegacyFavoriteRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.room.CoffeePeekDatabase
import com.coffeepeek.room.CoffeePeekDatabase.Companion.configure
import com.coffeepeek.room.repository.SettingRepositoryImp
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class FavoritesRoomMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun favoritesSurviveRoomV1AndV2MigrationsAndRemainWritable(): Unit = runBlocking {
        val oldSchemas = listOf(
            LegacySchema(version = 1, identityHash = "bba695680b6184c7f13ef7384a344b42"),
            LegacySchema(version = 2, identityHash = "d3476cf2c136a87a3537b2c7fab83894"),
        )

        oldSchemas.forEach { schema ->
            val name = "favorites-v${schema.version}-${UUID.randomUUID()}.db"
            try {
                seedDatabase(name, schema)

                val migrated = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
                val settings = SettingRepositoryImp(migrated.settingDAO)
                val app = koinApplication {
                    modules(favoritesRoomModule(settings, Dispatchers.IO), legacyFavoritesConsumersModule())
                }
                try {
                    val favorites = app.koin.get<FavoritesRepository>()
                    val legacy = app.koin.get<LegacyFavoriteRepository>()
                    val saved = favorites.read().getOrThrow()

                    assertEquals(listOf("saved-a", "saved-b"), saved.map { it.id })
                    assertEquals(listOf("logo-a"), saved.first().roasterPhotoUrls)
                    assertEquals(setOf("saved-a", "saved-b"), legacy.getFavoriteIds())

                    favorites.remove("saved-a").getOrThrow()
                    assertEquals(setOf("saved-b"), legacy.getFavoriteIds())
                    favorites.save(saved.first()).getOrThrow()
                    assertEquals(listOf("saved-a", "saved-b"), favorites.read().getOrThrow().map { it.id })
                    favorites.clear().getOrThrow()
                    assertNull(settings.read(FAVORITES_KEY))
                } finally {
                    app.close()
                    migrated.close()
                }

                val reopened = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
                try {
                    assertNull(SettingRepositoryImp(reopened.settingDAO).read(FAVORITES_KEY))
                } finally {
                    reopened.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
    }

    private fun seedDatabase(name: String, schema: LegacySchema) {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `room_master_table` (`id` INTEGER PRIMARY KEY, `identity_hash` TEXT)",
            )
            database.execSQL(
                "CREATE TABLE `setting_table` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            )
            if (schema.version == 2) createVersionTwoTables(database)

            database.execSQL(
                "INSERT INTO `room_master_table` (`id`, `identity_hash`) VALUES(42, ?)",
                arrayOf(schema.identityHash),
            )
            database.execSQL(
                "INSERT INTO `setting_table` (`key`, `value`) VALUES(?, ?)",
                arrayOf(FAVORITES_KEY, LEGACY_FAVORITES_JSON),
            )
            database.version = schema.version
        } finally {
            database.close()
        }
    }

    private fun createVersionTwoTables(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE `bean_bag` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `origin_country_code` TEXT NOT NULL,
                `roast_level` TEXT NOT NULL, `roaster_name` TEXT NOT NULL, `notes` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`)
            )""".trimIndent(),
        )
        database.execSQL(
            """CREATE TABLE `brew_session` (
                `id` TEXT NOT NULL, `bean_id` TEXT, `method` TEXT NOT NULL, `dose_g` REAL NOT NULL,
                `yield_or_water_g` REAL NOT NULL, `duration_sec` INTEGER NOT NULL, `temperature_c` REAL,
                `grind_note` TEXT NOT NULL, `taste_tags` TEXT NOT NULL, `overall_score` INTEGER,
                `advice_snapshot` TEXT NOT NULL, `notes` TEXT NOT NULL, `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`bean_id`) REFERENCES `bean_bag`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )""".trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_brew_session_bean_id` ON `brew_session` (`bean_id`)")
        database.execSQL("CREATE INDEX `index_brew_session_created_at` ON `brew_session` (`created_at`)")
        database.execSQL("CREATE INDEX `index_brew_session_method` ON `brew_session` (`method`)")
    }

    private data class LegacySchema(val version: Int, val identityHash: String)

    private companion object {
        const val FAVORITES_KEY = "local_favorite_shops"
        const val LEGACY_FAVORITES_JSON = """[
            {"id":"saved-a","title":"Старая кофейня","rating":4.8,
             "roasterPhotoUrl":"logo-a","unknownLegacyField":true},
            {"id":"saved-b","title":"Вторая кофейня"}
        ]"""
    }
}
