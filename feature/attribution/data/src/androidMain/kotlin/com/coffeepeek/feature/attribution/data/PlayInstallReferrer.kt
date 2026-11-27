package com.coffeepeek.feature.attribution.data

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun readPlayReferrer(context: Context): String? {
    val result = withTimeoutOrNull(5_000) { ReferrerResult(connectReferrer(context)) }
        ?: throw IllegalStateException("Install referrer timed out")
    return result.token
}

private data class ReferrerResult(val token: String?)

private suspend fun connectReferrer(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()
        continuation.invokeOnCancellation { client.endConnection() }
        startReferrer(client, continuation)
}

private fun startReferrer(client: InstallReferrerClient, continuation: CancellableContinuation<String?>) {
    try {
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (!continuation.isActive) return
                completeReferrer(client, responseCode, { continuation.resume(it) }, { continuation.resumeWithException(it) })
            }
            override fun onInstallReferrerServiceDisconnected() {
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Install referrer disconnected"))
                client.endConnection()
            }
        })
    } catch (error: Exception) {
        client.endConnection()
        if (continuation.isActive) continuation.resumeWithException(error)
    }
}

private fun completeReferrer(client: InstallReferrerClient, status: Int, success: (String?) -> Unit, failure: (Exception) -> Unit) {
    try {
        when (status) {
            InstallReferrerClient.InstallReferrerResponse.OK -> success(handoffFromReferrer(client.installReferrer.installReferrer))
            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> success(null)
            else -> failure(IllegalStateException("Install referrer unavailable"))
        }
    } catch (error: Exception) {
        failure(error)
    } finally {
        client.endConnection()
    }
}

