package com.coffeepeek.admin.location

import android.location.Location
import android.os.CancellationSignal
import android.os.OperationCanceledException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun awaitCurrentLocation(
    request: (CancellationSignal, (Location?) -> Unit) -> Unit,
): Location? = suspendCancellableCoroutine { continuation ->
    val cancellation = CancellationSignal()
    continuation.invokeOnCancellation { cancellation.cancel() }
    if (!continuation.isActive) return@suspendCancellableCoroutine

    try {
        request(cancellation) { location ->
            if (continuation.isActive) continuation.resume(location)
        }
    } catch (_: OperationCanceledException) {
        // Cancellation can race with registration even after the isActive check.
        // Leave a cancelled continuation cancelled so coroutine cancellation still propagates.
        if (continuation.isActive) continuation.resume(null)
    } catch (_: SecurityException) {
        if (continuation.isActive) continuation.resume(null)
    } catch (_: IllegalArgumentException) {
        if (continuation.isActive) continuation.resume(null)
    }
}
