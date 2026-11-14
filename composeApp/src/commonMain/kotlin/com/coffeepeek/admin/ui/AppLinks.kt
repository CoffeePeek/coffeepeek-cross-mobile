package com.coffeepeek.admin.ui

import io.ktor.http.Url
import io.ktor.http.encodeURLPathPart

internal fun appLinkScreen(link: String): Navigator.Screen? {
    val url = runCatching { Url(link) }.getOrNull() ?: return null
    if (url.protocol.name !in listOf("http", "https") ||
        !url.host.equals("coffeepeek.by", ignoreCase = true)
    ) return null

    if (url.encodedPath.trimEnd('/') == "/reset-password") return null

    val openCheckIn = url.parameters.getAll("checkin")?.firstOrNull() == "1"
    return appLinkDetail(url.segments, openCheckIn)
}

private fun appLinkDetail(path: List<String>, openCheckIn: Boolean): Navigator.Screen {
    if (path.size != 2) return Navigator.Screen.Main
    val slug = path[1]
    if (!validAppLinkSlug(slug)) return Navigator.Screen.Main

    return when (path[0]) {
        "coffee-shops", "shops" -> Navigator.Screen.ShopDetail(slug, openCheckIn = openCheckIn)
        "roasters" -> Navigator.Screen.RoasterDetail(slug)
        "coffees" -> Navigator.Screen.CoffeeDetail(slug)
        "users" -> Navigator.Screen.UserProfile(slug)
        "collections" -> if (Regex("[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(slug))
            Navigator.Screen.PublicCollection(slug) else Navigator.Screen.Main
        "check-ins" -> if (CHECK_IN_ID.matches(slug)) Navigator.Screen.CheckInDetail(slug) else Navigator.Screen.Main
        else -> Navigator.Screen.Main
    }
}

internal fun shopCheckInLink(slug: String): String? {
    if (slug.length > 200 || !validAppLinkSlug(slug)) return null
    return "https://coffeepeek.by/coffee-shops/${slug.encodeURLPathPart()}?checkin=1"
}

internal fun Navigator.Screen.appLinkDestination(isLoggedIn: Boolean): Navigator.Screen =
    if (this is Navigator.Screen.ShopDetail && openCheckIn && !isLoggedIn) Navigator.Screen.ShopCheckInSignIn(shopId)
    else profileDestination(isLoggedIn)

private fun validAppLinkSlug(slug: String): Boolean =
    slug.isNotBlank() && slug !in listOf(".", "..") && slug.none { it == '/' || it == '\\' || it.isISOControl() }

/** Post links carry the server's GUID; anything else is not a post the API could return. */
private val CHECK_IN_ID = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
