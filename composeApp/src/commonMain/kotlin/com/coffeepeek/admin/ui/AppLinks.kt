package com.coffeepeek.admin.ui

import io.ktor.http.Url

internal fun appLinkScreen(link: String): Navigator.Screen? {
    val url = runCatching { Url(link) }.getOrNull() ?: return null
    if (url.protocol.name !in listOf("http", "https") ||
        !url.host.equals("coffeepeek.by", ignoreCase = true)
    ) return null

    if (url.encodedPath.trimEnd('/') == "/reset-password") return null

    val path = url.segments
    if (path.size != 2) return Navigator.Screen.Main
    val slug = path[1]
    if (slug.isBlank() || slug.any { it == '/' || it == '\\' || it.isISOControl() } || slug in listOf(".", "..")) {
        return Navigator.Screen.Main
    }

    return when (path[0]) {
        "coffee-shops" -> Navigator.Screen.ShopDetail(slug)
        "roasters" -> Navigator.Screen.RoasterDetail(slug)
        "users" -> Navigator.Screen.UserProfile(slug)
        else -> Navigator.Screen.Main
    }
}
