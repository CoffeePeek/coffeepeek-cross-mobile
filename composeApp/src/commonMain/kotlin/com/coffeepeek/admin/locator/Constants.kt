package com.coffeepeek.admin.locator

import com.coffeepeek.admin.config.AppConfig

object Constants {
    val BASE_URL: String get() = AppConfig.baseUrl
    /** Kept as is so existing installs keep their database. */
    const val DB_NAME = "fp.db"
}
