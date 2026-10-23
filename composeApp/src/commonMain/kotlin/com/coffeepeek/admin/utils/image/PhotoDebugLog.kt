package com.coffeepeek.admin.utils.image

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.utils.CustomUrl

// TEMPORARY diagnostics for the photo viewer that sometimes keeps spinning (logcat tag CoffeePeek-PHOTO, debug builds only). Remove after the cause is found.
internal expect fun platformPhotoLog(message: String)

internal fun photoLog(message: String) {
    if (AppConfig.isDebug) platformPhotoLog(message)
}

/** A short, readable id of a photo url: the last three path segments. */
internal fun photoId(value: Any?): String {
    val url = when (value) {
        null -> return "-"
        is CustomUrl -> value.url
        else -> value.toString()
    }
    if (url.isBlank()) return "<blank>"
    val parts = url.substringBefore('?').trimEnd('/').split('/')
    return parts.takeLast(if ("/variants/" in url) 2 else 1).joinToString("/").let { name ->
        val id = parts.getOrNull(parts.indexOf("photos") + 1)?.take(8)
        if (id != null && "/variants/" in url) "$id:$name" else if (id != null) "$id:original" else name.takeLast(40)
    }
}
