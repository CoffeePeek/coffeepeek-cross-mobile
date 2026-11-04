package com.coffeepeek.admin.di.collections

internal data class CollectionPickerLease(val owner: String, val generation: Long)

/** Told about a successful shop favorite so the platform can offer the collection picker; no listener means no prompt. */
internal interface CollectionPickerPrompts {
    fun lease(account: String): CollectionPickerLease?
    fun added(lease: CollectionPickerLease?, slug: String)
}
