package com.coffeepeek.core.datetime

import java.time.Instant
import kotlin.time.ExperimentalTime

/** [parseServerInstant] for code that works with `java.time`. */
@OptIn(ExperimentalTime::class)
fun parseServerJavaInstant(raw: String): Instant? =
    parseServerInstant(raw)?.let { Instant.ofEpochSecond(it.epochSeconds, it.nanosecondsOfSecond.toLong()) }
