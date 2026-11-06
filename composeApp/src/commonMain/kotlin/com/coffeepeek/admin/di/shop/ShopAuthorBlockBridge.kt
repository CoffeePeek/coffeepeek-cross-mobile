package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.shop.api.ShopCheckInAuthor

/** Adapt independent feature APIs only while the originating account/visibility lease is current. */
internal fun ShopBrowsingSession.requestAuthorBlock(author: ShopCheckInAuthor, actions: AuthorBlockActions) {
    if (!isCurrent()) { revoke(); return }
    if (author.id.isNullOrBlank() && author.slug.isNullOrBlank()) return
    actions.requestBlock(AuthorReference(author.id, author.slug, author.nickname, author.avatarUrl))
}
