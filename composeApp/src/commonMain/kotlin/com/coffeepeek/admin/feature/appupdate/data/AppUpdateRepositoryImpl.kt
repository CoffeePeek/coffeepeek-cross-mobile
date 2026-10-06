package com.coffeepeek.admin.feature.appupdate.data

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.Url
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable

@Serializable
internal data class AppVersionResponse(val isSuccess: Boolean, val data: AppVersionDto? = null)

@Serializable
internal data class AppVersionDto(val versionCode: Long, val minVersionCode: Long, val url: String)

internal fun AppVersionResponse.toUpdate(): AppUpdate? {
    val dto = data ?: return null
    val url = Url(dto.url)
    if (!isSuccess || dto.minVersionCode < 0 || dto.versionCode < dto.minVersionCode ||
        url.protocol.name != "https" || url.host.isBlank() || !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()
    ) return null
    return AppUpdate(dto.versionCode, dto.minVersionCode, dto.url)
}

internal class AppUpdateRepositoryImpl(
    private val client: HttpClient,
    private val settings: SettingRepository,
) : AppUpdateRepository {
    override suspend fun fetch(): AppUpdate? = withTimeout(10_000) {
        val response = client.get("/api/v1/app-version") {
            parameter("platform", AppConfig.updatePlatform)
            AppConfig.updateChannel?.let { parameter("channel", it) }
            header("Cache-Control", "no-store")
        }
        if (response.status.value != 200) return@withTimeout null
        response.body<AppVersionResponse>().toUpdate()
    }

    override suspend fun dismissedVersion() = settings.read("dismissed_app_version")?.value?.toLongOrNull()
    override suspend fun dismiss(version: Long) = settings.save(Setting("dismissed_app_version", version.toString()))
}
