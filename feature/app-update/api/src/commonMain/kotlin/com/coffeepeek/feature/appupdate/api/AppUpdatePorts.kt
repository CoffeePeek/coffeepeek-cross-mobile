package com.coffeepeek.feature.appupdate.api

/** What the app tells the update feature about itself. */
interface AppUpdateEnvironment {
    /** The installed version code, or null when the build does not know it (then no update is ever offered). */
    val versionCode: Long?

    /** Sent as `platform` to the version endpoint: `android` or `ios`. */
    val platform: String

    /** Sent as `channel` when not null: `apk` or `play` on Android. */
    val channel: String?
}

/** Where the version the user dismissed with «Позже» is remembered across launches. */
interface DismissedUpdateStore {
    suspend fun read(): Long?
    suspend fun write(version: Long)
}
