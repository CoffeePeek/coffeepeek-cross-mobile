package com.coffeepeek.feature.shop.impl.ui.data

internal data class ContactLink(val label: String, val target: String)

internal fun websiteLink(raw: String?): ContactLink? {
    val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val target = when {
        value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true) -> value
        "://" in value || ':' in value -> return null
        else -> "https://$value"
    }
    if (target.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7F }) return null
    val host = target.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
    if (!validHost(host)) return null
    val label = target.substringAfter("://").removePrefix("www.")
        .substringBefore('?').substringBefore('#').trimEnd('/')
    return ContactLink(label, target)
}

internal fun instagramLink(raw: String?): ContactLink? {
    val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val path = when {
        value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true) -> {
            val host = value.substringAfter("://").substringBefore('/')
            if (!host.equals("instagram.com", ignoreCase = true) &&
                !host.equals("www.instagram.com", ignoreCase = true)) return null
            value.substringAfter("://").substringAfter('/', "")
        }
        value.startsWith("instagram.com/", ignoreCase = true) -> value.substringAfter('/')
        value.startsWith("www.instagram.com/", ignoreCase = true) -> value.substringAfter('/')
        else -> value.removePrefix("@")
    }
    val handle = path.substringBefore('/').substringBefore('?').substringBefore('#')
    if (!handle.matches(Regex("[A-Za-z0-9._]{1,30}"))) return null
    return ContactLink("@$handle", "https://instagram.com/$handle")
}

internal fun phoneLink(raw: String?): ContactLink? {
    val label = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (label.any { !it.isDigit() && it !in "+()- " }) return null
    val number = label.filter { it.isDigit() || it == '+' }
    if (number.count(Char::isDigit) < 3 || number.drop(1).contains('+')) return null
    return ContactLink(label, "tel:$number")
}

internal fun emailLink(raw: String?): ContactLink? {
    val address = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (address.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7F || it == ':' } ||
        address.count { it == '@' } != 1) return null
    val domain = address.substringAfter('@')
    if (!validHost(domain)) return null
    return ContactLink(address, "mailto:$address")
}

private fun validHost(host: String): Boolean = host.contains('.') && host.none { it.isWhitespace() } &&
    host.matches(Regex("[A-Za-z0-9.-]+")) && !host.startsWith('.') && !host.endsWith('.')
