package com.coffeepeek.admin.utils.image

import com.coffeepeek.admin.utils.CustomUrl
import com.coffeepeek.data.util.JwtUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.coroutines.sync.withPermit

internal suspend fun protectedVariantBytes(client: HttpClient, data: CustomUrl): ByteArray {
    check(client.pluginOrNull(HttpCache) == null) { "Protected images require an uncached transport" }
    val scope = checkNotNull(data.permissionScope) { "Фото недоступно" }
    val session = ProtectedPhotoSession.session(scope)
    val jwtDeadline = session?.accessToken?.let(JwtUtils::extractExp)?.takeIf { it in 0..Long.MAX_VALUE / 1000 }?.times(1000)
    check(session == null || jwtDeadline != null) { "Фото недоступно" }
    val result = protectedPhotoCache.read(PhotoCacheKey(data.url, scope), jwtDeadline, { ProtectedPhotoSession.current(scope) }) { etag ->
        protectedPhotoRequests.withPermit {
            check(ProtectedPhotoSession.current(scope)) { "Фото недоступно" }
            client.prepareGet(data.url) {
                expectSuccess = false
                // Bypass implicit Ktor/platform cache. Only our private byte store owns reuse.
                headers.append(HttpHeaders.CacheControl, "no-cache, no-store")
                etag?.let { headers.append(HttpHeaders.IfNoneMatch, it) }
            }.execute { response ->
            val bytes = if (response.status.value == 200) {
                val type = response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()
                check(type in setOf("image/jpeg", "image/png", "image/webp")) { "Фото недоступно" }
                val channel = response.bodyAsChannel()
                try { channel.readRemaining(8L * 1024 * 1024 + 1).readByteArray().also {
                    check(it.size <= 8 * 1024 * 1024) { "Фото недоступно" }
                } } finally { channel.cancel(null) }
            } else byteArrayOf()
            PhotoTransfer(response.status.value, response.headers, bytes)
            }
        }
    }
    check(ProtectedPhotoSession.current(scope)) { "Фото недоступно" }
    data.displayPermission?.grant(result.permission)
    return result.bytes
}

internal fun grantOnlinePhoto(data: CustomUrl, requestStart: Long) {
    val scope = checkNotNull(data.permissionScope) { "Фото недоступно" }
    check(ProtectedPhotoSession.current(scope)) { "Фото недоступно" }
    val nowMono = SystemPhotoCacheClock.monotonicMillis()
    val nowWall = SystemPhotoCacheClock.wallMillis()
    val jwt = ProtectedPhotoSession.session(scope)?.accessToken?.let(JwtUtils::extractExp)
        ?.takeIf { it in 0..Long.MAX_VALUE / 1000 }?.times(1000)
    check(ProtectedPhotoSession.session(scope) == null || jwt != null) { "Фото недоступно" }
    val lifetime = minOf((20_000 - (nowMono - requestStart)).coerceAtLeast(0), jwt?.let { (it - nowWall).coerceAtLeast(0) } ?: 20_000)
    check(lifetime > 0) { "Фото недоступно" }
    data.displayPermission?.grant(PhotoPermission(nowMono + lifetime, nowWall + lifetime))
}
