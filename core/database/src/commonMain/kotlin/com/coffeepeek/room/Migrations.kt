package com.coffeepeek.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Legacy: brew journal tables (removed in v3). Kept so installs on v1 can step 1→2→3. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bean_bag` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `origin_country_code` TEXT NOT NULL,
                `roast_level` TEXT NOT NULL,
                `roaster_name` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `brew_session` (
                `id` TEXT NOT NULL,
                `bean_id` TEXT,
                `method` TEXT NOT NULL,
                `dose_g` REAL NOT NULL,
                `yield_or_water_g` REAL NOT NULL,
                `duration_sec` INTEGER NOT NULL,
                `temperature_c` REAL,
                `grind_note` TEXT NOT NULL,
                `taste_tags` TEXT NOT NULL,
                `overall_score` INTEGER,
                `advice_snapshot` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`bean_id`) REFERENCES `bean_bag`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_brew_session_bean_id` ON `brew_session` (`bean_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_brew_session_created_at` ON `brew_session` (`created_at`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_brew_session_method` ON `brew_session` (`method`)")
    }
}

/** Drop brew journal tables — feature removed. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `brew_session`")
        connection.execSQL("DROP TABLE IF EXISTS `bean_bag`")
    }
}
