package com.coffeepeek.admin.di.collections

internal actual class CollectionPickerLock actual constructor() {
    private val monitor = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
