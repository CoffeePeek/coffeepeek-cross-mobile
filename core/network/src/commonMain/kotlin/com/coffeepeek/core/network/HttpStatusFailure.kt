package com.coffeepeek.core.network

/** HTTP status retained by a response validator; lets migrated data preserve diagnostics without importing legacy exceptions. */
interface HttpStatusFailure {
    val statusCode: Int?
}
