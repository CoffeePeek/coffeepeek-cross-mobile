package com.coffeepeek.feature.auth.data

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

/** The refresh token the server sets as a `refreshToken` cookie, when it does. */
internal fun HttpResponse.refreshTokenCookie(): String? {
    val setCookies = headers.getAll(HttpHeaders.SetCookie)
        ?: headers[HttpHeaders.SetCookie]?.let { listOf(it) }
        ?: return null
    return setCookies.firstNotNullOfOrNull { header -> cookieValue(header, "refreshToken") }
}

private fun cookieValue(setCookieHeader: String, cookieName: String): String? {
    val prefix = "$cookieName="
    val candidates = buildList {
        add(setCookieHeader.trim())
        setCookieHeader.split(',').mapTo(this) { it.trim() }
    }
    for (candidate in candidates) {
        val startIndex = candidate.indexOf(prefix, ignoreCase = true)
        if (startIndex < 0) continue
        val rawValue = candidate.substring(startIndex + prefix.length).substringBefore(';').trim()
        if (rawValue.isNotBlank()) return rawValue.percentDecoded()
    }
    return null
}

private fun String.percentDecoded(): String {
    val out = StringBuilder(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char == '%' && index + 2 < length) {
            out.append(substring(index + 1, index + 3).toInt(16).toChar())
            index += 3
        } else {
            out.append(char)
            index++
        }
    }
    return out.toString()
}
