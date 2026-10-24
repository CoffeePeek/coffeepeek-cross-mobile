package com.coffeepeek.feature.appupdate.domain

data class AppUpdate(val versionCode: Long, val minVersionCode: Long, val url: String) {
    fun isRequired(current: Long) = current < minVersionCode
    fun isAvailable(current: Long) = current < versionCode
    fun shouldPrompt(current: Long, dismissedVersion: Long?, manual: Boolean = false) =
        isAvailable(current) && (isRequired(current) || manual || dismissedVersion != versionCode)
}
