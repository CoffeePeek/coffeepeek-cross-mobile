package com.coffeepeek.admin.ui

import io.ktor.http.Url

internal fun appLinkScreen(link: String): Navigator.Screen? {
    val url = runCatching { Url(link) }.getOrNull() ?: return null
    if (url.protocol.name !in listOf("http", "https") ||
        !url.host.equals("coffeepeek.by", ignoreCase = true)
    ) return null

    val match = Regex("^/coffee-shops/([1-9][0-9]*)(?:-[^/]+)?/?$")
        .matchEntire(url.encodedPath)
    return match?.let { Navigator.Screen.ShopDetail(it.groupValues[1]) }
        ?: Navigator.Screen.Main
}
