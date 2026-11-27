package com.coffeepeek.feature.attribution.data

import android.content.Context
import android.os.Build
import com.coffeepeek.feature.attribution.domain.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual fun platformStore(directory: String): AttributionStore = AndroidAttributionStore(File(directory, "acquisition-v1.json"))

private class AndroidAttributionStore(private val file: File) : AttributionStore {
    override suspend fun load() = withContext(Dispatchers.IO) {
        decodeAttribution(if (file.isFile && file.length() <= 131072) file.readText() else null)
    }
    override suspend fun save(state: AttributionLocalState) = withContext(Dispatchers.IO) {
        check(file.parentFile?.let { it.isDirectory || it.mkdirs() } == true)
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(encodeAttribution(state))
        check(temporary.renameTo(file))
    }
}

internal actual fun platformInstallChannel(context: Any?): String {
    val app = (context as? Context)?.applicationContext ?: return "unknown"
    val installer = try {
        if (Build.VERSION.SDK_INT >= 30) app.packageManager.getInstallSourceInfo(app.packageName).installingPackageName
        else @Suppress("DEPRECATION") app.packageManager.getInstallerPackageName(app.packageName)
    } catch (_: Exception) { null }
    return if (installer == "com.android.vending") "google_play" else "unknown"
}

internal actual fun platformInstallSource(context: Any?): AttributionInstallSource {
    val app = (context as? Context)?.applicationContext
    return AttributionInstallSource {
        if (app == null || platformInstallChannel(app) != "google_play") null else readPlayReferrer(app)
    }
}
