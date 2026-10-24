package com.coffeepeek.admin.di.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.coffeepeek.admin.auth.IosGoogleAuthBridge
import com.coffeepeek.feature.auth.api.GoogleSignInLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal object IosGoogleSignInLauncher : GoogleSignInLauncher {
    @Composable
    override fun rememberSignIn(onToken: (String) -> Unit, onFailure: (String) -> Unit): () -> Unit {
        val scope = rememberCoroutineScope()
        val token = rememberUpdatedState(onToken)
        val failure = rememberUpdatedState(onFailure)
        val request = remember(scope) { IosGoogleRequest(scope) }
        return { request.launch({ token.value(it) }, { failure.value(it) }) }
    }
}

/** The scope belongs to this screen; leaving it drops late native results and never starts a backend request. */
private class IosGoogleRequest(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun launch(onToken: (String) -> Unit, onFailure: (String) -> Unit) {
        if (job?.isActive == true) return
        job = scope.launch {
            IosGoogleAuthBridge.signIn().fold(onToken) { failure ->
                if (failure is CancellationException) throw failure
                // This is the existing Swift bridge's cancellation value, not a provider/network failure.
                if (failure.message != "Вход через Google отменён") onFailure(failure.message.orEmpty())
            }
        }
    }
}
