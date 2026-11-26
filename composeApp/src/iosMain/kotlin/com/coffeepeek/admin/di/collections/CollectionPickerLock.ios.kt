package com.coffeepeek.admin.di.collections

import platform.Foundation.NSRecursiveLock

internal actual class CollectionPickerLock actual constructor() {
    private val lock = NSRecursiveLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try { block() } finally { lock.unlock() }
    }
}
