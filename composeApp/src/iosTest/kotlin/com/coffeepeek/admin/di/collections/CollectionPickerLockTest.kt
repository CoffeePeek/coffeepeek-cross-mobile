package com.coffeepeek.admin.di.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CollectionPickerLockTest {
    @Test fun requestLeaseAccountCallbacksCanReenterTheNativeLock() {
        val lock = CollectionPickerLock()
        assertEquals(7, lock.withLock { lock.withLock { 7 } })
    }

    @Test fun failedCallbackReleasesTheNativeLock() {
        val lock = CollectionPickerLock()
        assertFailsWith<IllegalStateException> { lock.withLock { error("fixture") } }
        assertEquals(9, lock.withLock { 9 })
    }
}
