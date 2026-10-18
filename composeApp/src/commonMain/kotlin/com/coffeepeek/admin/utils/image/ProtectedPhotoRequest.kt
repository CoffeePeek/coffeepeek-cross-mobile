package com.coffeepeek.admin.utils.image

import kotlinx.coroutines.sync.Semaphore

internal val protectedPhotoRequests = Semaphore(4)
internal fun isProtectedCheckInPhoto(url: String): Boolean =
    Regex("/api/v1/check-ins/[^/?]+/photos/[^/?]+").containsMatchIn(url)

internal fun originalCheckInPhoto(url: String): String? =
    url.takeIf { isProtectedCheckInPhoto(it) && "/variants/" in it }?.substringBefore("/variants/")
