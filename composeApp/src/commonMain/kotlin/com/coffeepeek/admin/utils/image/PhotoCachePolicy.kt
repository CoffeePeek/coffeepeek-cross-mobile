package com.coffeepeek.admin.utils.image

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.fromHttpToGmtDate

/** Only explicit private variant policy may retain bytes; malformed metadata fails closed. */
internal data class PhotoCachePolicy(
    val stores: Boolean,
    val maxAgeMillis: Long,
    val freshMillis: Long,
    val hasValidDate: Boolean = false,
    val onlineMillis: Long = 0,
    val allowsOnlineDisplay: Boolean = true,
) {
    companion object {
        fun read(headers: Headers, requestMono: Long, responseMono: Long, responseWall: Long): PhotoCachePolicy {
            val values = headers.getAll(HttpHeaders.CacheControl)?.joinToString(",") ?: return denied(online = true)
            val directives = values.split(',').map { it.trim().lowercase() }
            if (directives.any { it == "no-store" || it.startsWith("no-store=") }) return denied(online = true)
            val maxAges = directives.filter { it.substringBefore('=').trim() == "max-age" }
            if (maxAges.size != 1) return denied()
            val rawAge = maxAges.single().substringAfter('=', "").trim().removeSurrounding("\"")
            if (rawAge.isEmpty() || rawAge.any { !it.isDigit() }) return denied()
            val seconds = rawAge.toLongOrNull()?.takeIf { it <= Long.MAX_VALUE / 1000 } ?: return denied()
            val vary = headers.getAll(HttpHeaders.Vary)?.joinToString(",")?.split(',')?.map { it.trim().lowercase() }.orEmpty()
            if (vary.any { it !in setOf("authorization", "cookie", "accept-encoding") }) return denied()
            // Private responses are never eligible for a shared/disk HTTP cache.
            if (directives.none { it == "private" }) return denied()
            val maxAge = seconds * 1000
            val dateValues = headers.getAll(HttpHeaders.Date)
            val date = dateValues?.singleOrNull()?.let { runCatching { it.fromHttpToGmtDate().timestamp }.getOrNull() }
            val ageValues = headers.getAll(HttpHeaders.Age)
            val ageText = ageValues?.singleOrNull()
            val age = if (ageValues == null) 0L else ageText?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
                ?.toLongOrNull()?.takeIf { it <= Long.MAX_VALUE / 1000 }?.times(1000)
            if (age == null || responseMono < requestMono) return denied()
            if (date == null) return PhotoCachePolicy(true, maxAge, 0)
            val delay = responseMono - requestMono
            val apparentAge = (responseWall - date).coerceAtLeast(0)
            val correctedAge = age.plusSaturated(delay)
            val online = (minOf(maxAge, 20_000) - maxOf(apparentAge, correctedAge)).coerceAtLeast(0)
            val fresh = if (directives.any { it == "no-cache" || it.startsWith("no-cache=") }) 0 else online
            return PhotoCachePolicy(true, maxAge, fresh, true, online)
        }
        private fun denied(online: Boolean = false) = PhotoCachePolicy(false, 0, 0, allowsOnlineDisplay = online)
    }
}

private fun Long.plusSaturated(other: Long): Long = if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other
