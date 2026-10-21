package com.coffeepeek.feature.roaster.impl.ui.data

// Link rules copied 1:1 from the legacy roaster screen.
internal fun normalizeInstagramUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) return trimmed
    val handle = trimmed.removePrefix("@").trim('/')
    return "https://instagram.com/$handle"
}

internal fun normalizeExternalUrl(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}

internal fun instagramDisplayText(value: String): String {
    val handle = normalizeInstagramUrl(value)
        .substringAfter("instagram.com/", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .trim()
    return if (handle.isNotBlank()) "@$handle" else prettyExternalLink(value)
}

internal fun prettyExternalLink(value: String): String = normalizeExternalUrl(value)
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore('?')
    .substringBefore('#')
    .trimEnd('/')
