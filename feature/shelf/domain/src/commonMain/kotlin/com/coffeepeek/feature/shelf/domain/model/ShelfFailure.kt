package com.coffeepeek.feature.shelf.domain.model

/** Stable reasons, not translated UI text or raw HTTP messages. */
enum class ShelfIssue {
    InvalidInput, BeanUnavailable, PhotoUpload, ShelfFull, Stale, NotFound,
    SignedOut, AccountChanged, Unauthorized, Offline, Timeout, RateLimited, Unavailable, InvalidResponse, RequestFailed,
}

/** The UI maps [issue] to its resources; transport details never escape through message or cause. */
class ShelfFailure(val issue: ShelfIssue, val statusCode: Int? = null) : Exception("Shelf: ${issue.name}")
