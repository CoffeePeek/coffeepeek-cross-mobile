package com.coffeepeek.admin.feature.appupdate.domain

internal data class AppUpdate(val versionCode: Long, val minVersionCode: Long, val url: String) {
    fun isRequired(current: Long) = current < minVersionCode
    fun isAvailable(current: Long) = current < versionCode
    fun shouldPrompt(current: Long, dismissedVersion: Long?, manual: Boolean = false) =
        isAvailable(current) && (isRequired(current) || manual || dismissedVersion != versionCode)
}

internal interface AppUpdateRepository {
    suspend fun fetch(): AppUpdate?
    suspend fun dismissedVersion(): Long?
    suspend fun dismiss(version: Long)
}
