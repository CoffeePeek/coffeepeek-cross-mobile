package com.coffeepeek.admin.utils.image

import com.coffeepeek.admin.utils.CustomUrl
import com.coffeepeek.domain.model.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** Opaque scope for in-flight work; credentials never become an image/cache key. */
@OptIn(ExperimentalAtomicApi::class)
internal object ProtectedPhotoSession {
    private class State(val session: Session?, val scope: Any = Any())
    private val state = AtomicReference(State(null))
    fun scope(session: Session?): Any {
        while (true) {
            val current = state.load()
            if (current.session == session) return current.scope
            val next = State(session)
            if (state.compareAndSet(current, next)) return next.scope
        }
    }
}

/** No completed-byte cache. Last-subscriber cancellation also stops a queued transfer. */
internal class ProtectedPhotoLoads(private val scope: CoroutineScope) {
    private class Entry(val result: CompletableDeferred<ByteArray>, val job: Job, var users: Int = 0)
    private val mutex = Mutex()
    private val entries = mutableMapOf<CustomUrl, Entry>()

    suspend fun read(key: CustomUrl, load: suspend () -> ByteArray): ByteArray {
        val entry = mutex.withLock {
            entries.getOrPut(key) {
                val result = CompletableDeferred<ByteArray>()
                val job = scope.launch(start = CoroutineStart.LAZY) {
                    try { result.complete(load()) }
                    catch (error: Throwable) {
                        result.completeExceptionally(error)
                        if (error is CancellationException) throw error
                    }
                    finally { withContext(NonCancellable) { mutex.withLock {
                        if (entries[key]?.result === result) entries.remove(key)
                    } } }
                }
                Entry(result, job)
            }.also { it.users++ }
        }
        entry.job.start()
        return try { entry.result.await() }
        finally { withContext(NonCancellable) { mutex.withLock {
            if (--entry.users == 0) {
                if (entries[key] === entry) entries.remove(key)
                entry.job.cancel()
            }
        } } }
    }
}

internal val protectedPhotoLoads = ProtectedPhotoLoads(CoroutineScope(SupervisorJob() + Dispatchers.Default))
