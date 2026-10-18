package com.coffeepeek.admin.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.OperationCanceledException
import androidx.core.location.LocationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrentLocationRequestTest {
    @Test
    fun returnsLocationFromCallback() = runBlocking {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 53.9
            longitude = 27.56
        }

        assertSame(location, awaitCurrentLocation { _, callback -> callback(location) })
        assertNull(awaitCurrentLocation { _, callback -> callback(null) })
    }

    @Test
    fun alreadyCancelledCoroutineDoesNotRegisterRequest() = runBlocking {
        var registered = false
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            awaitCurrentLocation { _, _ -> registered = true }
            error("Cancelled request must not return normally")
        }

        request.join()
        assertTrue(request.isCancelled)
        assertFalse(registered)
    }

    @Test
    fun cancellationDuringAndroidRequestRegistrationDoesNotCrash() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var platformThrewCancellation = false
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            val requestJob = currentCoroutineContext().job
            awaitCurrentLocation { cancellation, callback ->
                // Cancel after awaitCurrentLocation's isActive check, before the Android call.
                requestJob.cancel()
                assertTrue(cancellation.isCanceled)
                try {
                    LocationManagerCompat.getCurrentLocation(
                        manager, LocationManager.GPS_PROVIDER, cancellation, Executor { it.run() },
                    ) { location -> callback(location) }
                } catch (error: OperationCanceledException) {
                    platformThrewCancellation = true
                    throw error
                }
            }
            error("Cancelled request must not return normally")
        }

        request.join()
        assertTrue(platformThrewCancellation)
        assertTrue(request.isCancelled)
    }

    @Test
    fun timeoutCancelsPlatformRequestAndReturnsNull() = runBlocking {
        lateinit var cancellation: CancellationSignal
        var lateCallback: ((Location?) -> Unit)? = null

        val result = withTimeoutOrNull(100) {
            awaitCurrentLocation { signal, callback ->
                cancellation = signal
                lateCallback = callback
            }
        }

        assertNull(result)
        assertTrue(cancellation.isCanceled)
        lateCallback!!(Location(LocationManager.GPS_PROVIDER))
    }

    @Test
    fun callerCancellationCancelsPlatformRequestAndIgnoresLateCallback() = runBlocking {
        lateinit var cancellation: CancellationSignal
        var lateCallback: ((Location?) -> Unit)? = null
        var completionCause: Throwable? = null
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            awaitCurrentLocation { signal, callback ->
                cancellation = signal
                lateCallback = callback
            }
            error("Cancelled request must not return normally")
        }
        request.invokeOnCompletion { completionCause = it }

        request.cancel()
        lateCallback!!(Location(LocationManager.GPS_PROVIDER))
        request.join()

        assertTrue(cancellation.isCanceled)
        assertTrue(completionCause is CancellationException)
    }

    @Test
    fun platformCancellationWithoutCoroutineCancellationReturnsNull() = runBlocking {
        assertNull(awaitCurrentLocation { _, _ -> throw OperationCanceledException() })
    }

    @Test
    fun revokedPermissionReturnsNull() = runBlocking {
        assertNull(awaitCurrentLocation { _, _ -> throw SecurityException() })
    }

    @Test
    fun unavailableProviderReturnsNull() = runBlocking {
        assertNull(awaitCurrentLocation { _, _ -> throw IllegalArgumentException() })
    }
}
