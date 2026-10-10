package com.coffeepeek.core.network

import io.ktor.http.Url

/** Reject presigned URLs pointing to local/private hosts before sending image bytes. */
fun requirePublicUploadUrl(url: String) {
    val host = try { Url(url).host.lowercase().trimEnd('.') } catch (_: Exception) {
        throw IllegalArgumentException("Invalid upload URL")
    }
    require(url.startsWith("https://") || url.startsWith("http://")) { "Invalid upload URL" }
    require(host.isNotBlank() && host != "localhost" && host != "minio" &&
        ':' !in host && !host.endsWith(".internal") &&
        !host.endsWith(".local") && !isBlockedIpv4(host) &&
        (host.contains('.') || isIpv4(host))) { "Private upload URL" }
}

private fun isIpv4(host: String): Boolean {
    val parts = host.split('.')
    return parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
}

private fun isBlockedIpv4(host: String): Boolean {
    val parts = host.split('.').mapNotNull(String::toIntOrNull)
    if (parts.size != 4) return false
    val first = parts[0]
    val second = parts[1]
    return first == 0 || first == 10 || first == 127 || first >= 224 ||
        first == 100 && second in 64..127 || first == 169 && second == 254 ||
        first == 172 && second in 16..31 || first == 192 && second == 168 ||
        first == 198 && second in 18..19
}
