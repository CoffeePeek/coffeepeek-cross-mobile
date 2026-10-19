package com.coffeepeek.admin.ui.component

import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.domain.model.CheckIn

internal fun CheckIn.blockableAuthor(): AuthorReference? =
    if (authorId.isNullOrBlank() && authorAddress?.slug.isNullOrBlank()) null
    else AuthorReference(authorId, authorAddress?.slug, username.ifBlank { "Автор" }, authorAvatarUrl)
