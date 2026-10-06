package com.coffeepeek.admin.feature.appupdate.domain

import kotlinx.coroutines.flow.StateFlow

internal enum class InstallationStage { Available, Starting, Downloading, Verifying, Ready, Installing }

internal data class InstallationState(
    val update: AppUpdate? = null,
    val stage: InstallationStage = InstallationStage.Available,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null,
    val canCancel: Boolean = false,
) {
    val progress: Float?
        get() = if (totalBytes > 0) (downloadedBytes.toDouble() / totalBytes).coerceIn(0.0, 1.0).toFloat() else null
    val busy: Boolean
        get() = stage == InstallationStage.Starting || stage == InstallationStage.Downloading ||
            stage == InstallationStage.Verifying || stage == InstallationStage.Installing
}

internal interface UpdateInstaller {
    val state: StateFlow<InstallationState>
    fun download(update: AppUpdate)
    fun install()
    fun cancel()
    fun refresh()
}

internal fun isCompatibleUpdate(
    expectedPackage: String,
    expectedVersion: Long,
    currentVersion: Long,
    downloadedPackage: String,
    downloadedVersion: Long,
    installedSigners: Set<String>,
    downloadedSigners: Set<String>,
    downloadedHistory: Set<String>,
): Boolean = downloadedPackage == expectedPackage && downloadedVersion == expectedVersion &&
    downloadedVersion > currentVersion && installedSigners.isNotEmpty() && downloadedSigners.isNotEmpty() &&
    if (installedSigners.size > 1 || downloadedSigners.size > 1) installedSigners == downloadedSigners
    else downloadedHistory.containsAll(installedSigners)
