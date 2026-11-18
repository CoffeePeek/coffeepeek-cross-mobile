package com.coffeepeek.admin.di.collections

/** Reentrant synchronization for request/account callbacks arriving from different platform threads. */
internal expect class CollectionPickerLock() {
    fun <T> withLock(block: () -> T): T
}
