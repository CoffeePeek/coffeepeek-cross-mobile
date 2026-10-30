package com.coffeepeek.room

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.room.CoffeePeekDatabase.Companion.configure
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepositoryImp
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The application database as installed builds shipped it: each exported schema version is created by hand with its real
 * identity hash, then opened through the production builder. Migration must keep the settings rows (the only data the
 * database holds), leave no retired journal table behind and never fall back to dropping the database.
 */
@RunWith(AndroidJUnit4::class)
class ApplicationDatabaseMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun settingsSurviveEveryShippedSchemaAndStayWritable(): Unit = runBlocking {
        for (schema in listOf(
            ShippedSchema(version = 1, identityHash = "bba695680b6184c7f13ef7384a344b42"),
            ShippedSchema(version = 2, identityHash = "d3476cf2c136a87a3537b2c7fab83894"),
        )) {
            val name = "application-v${schema.version}-${UUID.randomUUID()}.db"
            try {
                seed(name, schema)

                val migrated = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
                try {
                    val settings = SettingRepositoryImp(migrated.settingDAO)
                    assertEquals(listOf(Setting(CITY_KEY, "minsk"), Setting(JSON_KEY, JSON_VALUE)).sortedBy { it.key },
                        settings.readAll().sortedBy { it.key })
                    settings.save(Setting(CITY_KEY, "brest"))
                    settings.save(Setting(NEW_KEY, "written after migration"))
                } finally { migrated.close() }

                val reopened = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
                try {
                    val settings = SettingRepositoryImp(reopened.settingDAO)
                    assertEquals("brest", settings.read(CITY_KEY)?.value)
                    assertEquals(JSON_VALUE, settings.read(JSON_KEY)?.value)
                    assertEquals("written after migration", settings.read(NEW_KEY)?.value)
                    settings.delete(NEW_KEY)
                    assertNull(settings.read(NEW_KEY))
                } finally { reopened.close() }

                assertEquals(listOf("setting_table"), userTables(name))
            } finally { context.deleteDatabase(name) }
        }
    }

    @Test
    fun aNewInstallCreatesTheCurrentSchemaOnly(): Unit = runBlocking {
        val name = "application-new-${UUID.randomUUID()}.db"
        try {
            val created = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
            try {
                val settings = SettingRepositoryImp(created.settingDAO)
                assertTrue(settings.readAll().isEmpty())
                settings.save(Setting(CITY_KEY, "minsk"))
            } finally { created.close() }
            assertEquals(listOf("setting_table"), userTables(name))
        } finally { context.deleteDatabase(name) }
    }

    private fun seed(name: String, schema: ShippedSchema) {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS `room_master_table` (`id` INTEGER PRIMARY KEY, `identity_hash` TEXT)")
            database.execSQL("CREATE TABLE `setting_table` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            if (schema.version == 2) createJournalTables(database)
            database.execSQL("INSERT INTO `room_master_table` (`id`, `identity_hash`) VALUES(42, ?)", arrayOf(schema.identityHash))
            database.execSQL("INSERT INTO `setting_table` (`key`, `value`) VALUES(?, ?)", arrayOf(CITY_KEY, "minsk"))
            database.execSQL("INSERT INTO `setting_table` (`key`, `value`) VALUES(?, ?)", arrayOf(JSON_KEY, JSON_VALUE))
            database.version = schema.version
        } finally { database.close() }
    }

    /** The brew journal tables that schema 2 carried and schema 3 dropped. */
    private fun createJournalTables(database: SQLiteDatabase) {
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

    private fun userTables(name: String): List<String> {
        val database = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' " +
                    "AND name NOT IN ('room_master_table', 'android_metadata') ORDER BY name", null,
            ).use { cursor ->
                return buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        } finally { database.close() }
    }

    private data class ShippedSchema(val version: Int, val identityHash: String)

    private companion object {
        const val CITY_KEY = "city"
        const val JSON_KEY = "legacy_json_setting"
        const val NEW_KEY = "written_after_migration"
        const val JSON_VALUE = """[{"id":"a","unknownLegacyField":true},{"id":"b"}]"""
    }
}
