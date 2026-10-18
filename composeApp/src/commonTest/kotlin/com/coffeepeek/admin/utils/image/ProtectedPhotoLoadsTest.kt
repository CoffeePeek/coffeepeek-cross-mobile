package com.coffeepeek.admin.utils.image

import com.coffeepeek.admin.utils.CustomUrl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProtectedPhotoLoadsTest {
    @Test
    fun duplicatesShareOnlyInFlightBytesAndOneCancellationDoesNotCancelAnotherViewer() = runTest {
        val loads = ProtectedPhotoLoads(backgroundScope)
        val key = CustomUrl("/api/v1/check-ins/visit/photos/photo/variants/source/1/feed.png", Any())
        val finish = CompletableDeferred<ByteArray>()
        var calls = 0
        suspend fun transfer(): ByteArray { calls++; return finish.await() }
        val one = async { loads.read(key, ::transfer) }
        val two = async { loads.read(key, ::transfer) }
        runCurrent()
        assertEquals(1, calls)
        one.cancelAndJoin()
        assertTrue(two.isActive)
        finish.complete(byteArrayOf(1, 2))
        assertEquals(listOf<Byte>(1, 2), two.await().toList())
        loads.read(key) { calls++; byteArrayOf(3) }
        assertEquals(2, calls)
    }

    @Test
    fun aNewSessionNeverJoinsAnOldAccountsTransfer() = runTest {
        val loads = ProtectedPhotoLoads(backgroundScope)
        val finish = CompletableDeferred<ByteArray>()
        val first = async { loads.read(CustomUrl("/photo", Any())) { finish.await() } }
        runCurrent()
        val second = async { loads.read(CustomUrl("/photo", Any())) { byteArrayOf(9) } }
        assertEquals(listOf<Byte>(9), second.await().toList())
        first.cancelAndJoin()
    }
}
