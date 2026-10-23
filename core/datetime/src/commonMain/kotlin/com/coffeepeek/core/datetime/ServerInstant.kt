package com.coffeepeek.core.datetime

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Reads a timestamp sent by the server. The server may leave the offset out, and such a value is UTC.
 * Blank or unreadable text gives null; an explicit offset is honoured.
 */
@OptIn(ExperimentalTime::class)
fun parseServerInstant(raw: String): Instant? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    runCatching { Instant.parse(trimmed) }.getOrNull()?.let { return it }
    return runCatching { Instant.parse("${trimmed}Z") }.getOrNull()
}
