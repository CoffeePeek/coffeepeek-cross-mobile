package com.coffeepeek.admin.base

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.utils.LoadingHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BaseViewModelTest {
    private class Subject : BaseViewModel() {
        fun start(onError: (Exception) -> Unit, request: suspend () -> Unit) {
            launchRequest(onError = onError, request = request)
        }
    }

    @Test fun clearingViewModelCancelsRequestWithoutPresentingAnError() = runBlocking {
        ErrorHandler.clearError()
        LoadingHandler.clearLoading()
        val store = ViewModelStore()
        val subject = Subject()
        store.put("subject", subject)
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val localError = CompletableDeferred<Exception>()

        try {
            subject.start(onError = { localError.complete(it) }) {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
            withTimeout(5_000) { started.await() }
            assertEquals(true, LoadingHandler.isLoading.value)

            store.clear()

            withTimeout(5_000) {
                cancelled.await()
                LoadingHandler.isLoading.first { !it }
            }
            assertFalse(localError.isCompleted)
            assertNull(ErrorHandler.errorMessage.value)
        } finally {
            store.clear()
            LoadingHandler.clearLoading()
            ErrorHandler.clearError()
        }
    }

    @Test fun failedRequestInvokesLocalErrorAndReleasesLoading() = runBlocking {
        ErrorHandler.clearError()
        LoadingHandler.clearLoading()
        val subject = Subject()
        val localError = CompletableDeferred<Exception>()

        try {
            subject.start(onError = { localError.complete(it) }) {
                throw IllegalStateException("request failed")
            }
            val error = withTimeout(5_000) { localError.await() }
            withTimeout(5_000) { LoadingHandler.isLoading.first { !it } }

            assertEquals("request failed", error.message)
            assertNull(ErrorHandler.errorMessage.value)
        } finally {
            subject.close()
            LoadingHandler.clearLoading()
            ErrorHandler.clearError()
        }
    }
}
