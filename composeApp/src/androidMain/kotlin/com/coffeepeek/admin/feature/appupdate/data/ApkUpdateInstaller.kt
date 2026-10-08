package com.coffeepeek.admin.feature.appupdate.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.InstallationStage
import com.coffeepeek.admin.feature.appupdate.domain.InstallationState
import com.coffeepeek.admin.feature.appupdate.domain.isCompatibleUpdate
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ApkUpdateInstaller(private val context: Context) : AndroidUpdateInstaller {
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val preferences = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(InstallationState())
    override val state = mutableState.asStateFlow()
    override var requestConsent: ((IntentSender) -> Unit)? = null
    override var requestInstallation: ((Intent) -> Unit)? = null
    private var monitor: Job? = null
    private var awaitingPermission = false

    private fun apkFile(update: AppUpdate): File {
        val folder = File(checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)), "updates")
        check(folder.isDirectory || folder.mkdirs())
        return File(folder, "coffeepeek-${update.versionCode}.apk")
    }

    override fun download(update: AppUpdate) {
        if (state.value.busy) return
        cancel()
        mutableState.value = InstallationState(update, InstallationStage.Starting, canCancel = true)
        try {
            val uri = Uri.parse(update.url)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null)
            val file = apkFile(update)
            check(!file.exists() || file.delete())
            val id = downloads.enqueue(
                DownloadManager.Request(uri)
                    .setTitle("Обновление CoffeePeek")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file)),
            )
            preferences.edit().putLong("id", id).putLong("version", update.versionCode)
                .putLong("minimum", update.minVersionCode).putString("url", update.url).apply()
            refresh()
        } catch (_: Exception) {
            cancel()
            mutableState.value = InstallationState(update, error = "Не удалось начать загрузку. Попробуйте ещё раз.")
        }
    }

    override fun refresh() {
        if (monitor?.isActive == true || state.value.stage == InstallationStage.Installing) return
        val id = preferences.getLong("id", -1)
        if (id < 0) return
        val update = AppUpdate(preferences.getLong("version", 0), preferences.getLong("minimum", 0), preferences.getString("url", "") ?: "")
        val installedVersion = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
        if (installedVersion >= update.versionCode) {
            cancel()
            return
        }
        if (state.value.stage == InstallationStage.Ready) return
        monitor = scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    ensureActive()
                    val status = downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                        check(cursor.moveToFirst())
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
                            mutableState.value = InstallationState(
                                update, InstallationStage.Downloading,
                                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                                canCancel = true,
                            )
                        }
                        status
                    }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            mutableState.value = InstallationState(update, InstallationStage.Verifying)
                            verifyApk(update)
                            ensureActive()
                            mutableState.value = InstallationState(update, InstallationStage.Ready)
                            return@launch
                        }
                        DownloadManager.STATUS_FAILED -> error("Download failed")
                    }
                    delay(500)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                ensureActive()
                downloads.remove(id)
                preferences.edit().clear().apply()
                runCatching { apkFile(update).delete() }
                mutableState.value = InstallationState(update, error = "Не удалось скачать или проверить обновление. Попробуйте ещё раз.")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyApk(update: AppUpdate) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val downloaded = checkNotNull(context.packageManager.getPackageArchiveInfo(apkFile(update).absolutePath, flags))
        fun signers(info: PackageInfo, history: Boolean = false): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) {
                val signing = checkNotNull(info.signingInfo)
                if (history && !signing.hasMultipleSigners()) signing.signingCertificateHistory else signing.apkContentsSigners
            } else info.signatures
            return signatures.orEmpty().map {
                MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
            }.toSet()
        }
        check(isCompatibleUpdate(
            context.packageName, update.versionCode, PackageInfoCompat.getLongVersionCode(installed),
            downloaded.packageName, PackageInfoCompat.getLongVersionCode(downloaded),
            signers(installed), signers(downloaded), signers(downloaded, history = true),
        ))
    }

    override fun install() {
        val update = state.value.update ?: return
        if (state.value.stage != InstallationStage.Ready || monitor?.isActive == true) return
        monitor = scope.launch {
            var verified = false
            try {
                withContext(Dispatchers.IO) { verifyApk(update) }
                verified = true
                val launch = checkNotNull(requestInstallation)
                if (!context.packageManager.canRequestPackageInstalls()) {
                    awaitingPermission = true
                    launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile(update))
                    mutableState.value = state.value.copy(stage = InstallationStage.Installing, error = null)
                    launch(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                awaitingPermission = false
                if (!verified) {
                    val id = preferences.getLong("id", -1)
                    if (id >= 0) downloads.remove(id)
                    preferences.edit().clear().apply()
                    runCatching { apkFile(update).delete() }
                }
                mutableState.value = InstallationState(update,
                    if (verified) InstallationStage.Ready else InstallationStage.Available,
                    error = "Не удалось установить обновление. Попробуйте ещё раз.")
            }
        }
    }

    override fun installationResult() {
        if (awaitingPermission) {
            awaitingPermission = false
            if (context.packageManager.canRequestPackageInstalls()) install()
            else mutableState.value = state.value.copy(error = "Разрешите CoffeePeek устанавливать обновления и нажмите «Установить».")
        } else mutableState.value = state.value.copy(stage = InstallationStage.Ready)
    }

    override fun consentResult(resultCode: Int) = Unit

    override fun cancel() {
        monitor?.cancel()
        monitor = null
        val id = preferences.getLong("id", -1)
        if (id >= 0) downloads.remove(id)
        val version = preferences.getLong("version", 0)
        if (version > 0) runCatching { apkFile(AppUpdate(version, 0, "")).delete() }
        preferences.edit().clear().apply()
        mutableState.value = InstallationState()
    }
}
