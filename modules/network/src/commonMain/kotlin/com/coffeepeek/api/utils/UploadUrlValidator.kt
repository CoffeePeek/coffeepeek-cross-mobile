package com.coffeepeek.api.utils

import com.coffeepeek.core.network.ApiException

object UploadUrlValidator {

    fun requirePublicUploadUrl(url: String) {
        val host = runCatching {
            io.ktor.http.Url(url).host
        }.getOrElse {
            throw ApiException("Некорректный URL для загрузки фото")
        }
        if (isInternalHost(host)) {
            throw ApiException(
                "Сервер вернул недоступный URL для загрузки ($host). Обратитесь в поддержку.",
            )
        }
    }

    private fun isInternalHost(host: String): Boolean {
        val normalized = host.lowercase()
        if (normalized.isBlank()) return true
        if (
            normalized == "localhost" ||
            normalized == "127.0.0.1" ||
            normalized == "::1" ||
            normalized == "minio"
        ) {
            return true
        }
        if (normalized.endsWith(".internal") || normalized.endsWith(".local")) return true
        if (!normalized.contains('.') && !isIpv4(normalized)) return true
        return isPrivateIpv4(normalized)
    }

    private fun isIpv4(host: String): Boolean =
        host.split('.').let { parts ->
            parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
        }

    private fun isPrivateIpv4(host: String): Boolean {
        val parts = host.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return when {
            parts[0] == 10 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            parts[0] == 192 && parts[1] == 168 -> true
            else -> false
        }
    }
}
