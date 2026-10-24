package com.coffeepeek.feature.appupdate.domain

interface AppUpdateRepository {
    suspend fun fetch(): AppUpdate?
    suspend fun dismissedVersion(): Long?
    suspend fun dismiss(version: Long)
}
