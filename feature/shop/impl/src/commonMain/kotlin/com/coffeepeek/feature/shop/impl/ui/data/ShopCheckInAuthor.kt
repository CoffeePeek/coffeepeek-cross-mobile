package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn

/** Match main's public-feed policy: auth user IDs are not public author IDs. */
internal fun ShopCheckIn.blockableAuthor(
    isOwn: Boolean,
    isLoggedIn: Boolean,
    currentAuthorSlug: String?,
): ShopCheckInAuthor? {
    if (isOwn || (isLoggedIn && currentAuthorSlug.isNullOrBlank()) ||
        (!currentAuthorSlug.isNullOrBlank() && currentAuthorSlug == authorSlug)) return null
    val id = authorId?.takeIf(String::isNotBlank)
    val slug = authorSlug?.takeIf(String::isNotBlank)
    if (id == null && slug == null) return null
    return ShopCheckInAuthor(id, slug, username, authorAvatarUrl)
}
