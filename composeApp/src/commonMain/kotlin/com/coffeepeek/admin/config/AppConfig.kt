package com.coffeepeek.admin.config

expect object AppConfig {
    val versionCode: Long?
    val updatePlatform: String
    val updateChannel: String?
    val versionName: String
    val baseUrl: String
    val postHogProjectToken: String
    val isDebug: Boolean
}
