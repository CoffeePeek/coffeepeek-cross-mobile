package com.coffeepeek.admin.utils

import kotlinx.coroutines.*

/** A picker delegate's child lifetime; the presenting screen may keep observing its revoked UI. */
internal class NativePhotoImportSession(parent: CoroutineScope, private val loading: (Boolean) -> Unit,
    private val picked: (List<PickedImage>) -> Unit,
) {
    private val lifetime = SupervisorJob(parent.coroutineContext[Job])
    private val scope = CoroutineScope(parent.coroutineContext + lifetime)
    private var work: Job? = null
    private var revision = 0L
    val active: Boolean get() = lifetime.isActive

    fun start(load: suspend () -> List<PickedImage>) {
        if (!active || work?.isActive == true) return
        val request = ++revision
        loading(true)
        work = scope.launch { prepare(request, load) }
    }

    private suspend fun prepare(request: Long, load: suspend () -> List<PickedImage>) {
        try {
            val images = load()
            if (current(request)) picked(images) else images.forEach { it.bytes.fill(0) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (current(request)) picked(emptyList()) }
        finally { if (current(request)) loading(false) }
    }

    fun cancelSelection() {
        if (!active) return
        revision++
        work?.cancel()
        work = null
        loading(false)
        picked(emptyList())
    }

    fun close() {
        revision++
        lifetime.cancel()
    }

    private fun current(request: Long) = active && revision == request
}
