package com.coffeepeek.feature.appupdate.data

import com.coffeepeek.feature.appupdate.domain.AppUpdate
import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionResponse(val isSuccess: Boolean, val data: AppVersionDto? = null)

@Serializable
data class AppVersionDto(val versionCode: Long, val minVersionCode: Long, val url: String)

fun AppVersionResponse.toUpdate(): AppUpdate? {
    val dto = data ?: return null
    val url = Url(dto.url)
    if (!isSuccess || dto.minVersionCode < 0 || dto.versionCode < dto.minVersionCode ||
        url.protocol.name != "https" || url.host.isBlank() || !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()
    ) return null
    return AppUpdate(dto.versionCode, dto.minVersionCode, dto.url)
}
