package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn

/** Blocking requires a resolved signed-in viewer; auth IDs are not public author IDs. */
internal fun ShopCheckIn.blockableAuthor(
    isOwn: Boolean,
    isLoggedIn: Boolean,
    currentAuthorSlug: String?,
): ShopCheckInAuthor? {
    if (!isLoggedIn || isOwn || currentAuthorSlug.isNullOrBlank() ||
        currentAuthorSlug == authorSlug) return null
    val id = authorId?.takeIf(String::isNotBlank)
    val slug = authorSlug?.takeIf(String::isNotBlank)
    if (id == null && slug == null) return null
    return ShopCheckInAuthor(id, slug, username, authorAvatarUrl)
}
