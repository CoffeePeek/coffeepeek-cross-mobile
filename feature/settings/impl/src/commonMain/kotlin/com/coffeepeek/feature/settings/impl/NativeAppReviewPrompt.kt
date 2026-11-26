package com.coffeepeek.feature.settings.impl

import kotlinx.coroutines.*

/** Native screen lifetime; eligibility, persistence and claiming use the existing platform policy. */
class NativeAppReviewPrompt internal constructor(private val platform: AppReviewPlatform, parent: CoroutineScope) {
    private val lifetime = SupervisorJob(parent.coroutineContext[Job])
    private val scope = CoroutineScope(parent.coroutineContext + lifetime)
    private var attempt: Job? = null
    private var active = false
    private var closed = false
    fun presentation(eligible: Boolean) {
        if (closed || eligible == active) return
        active = eligible
        attempt?.cancel()
        attempt = null
        if (eligible) attempt = scope.launch { platform.promptWhenEligible { active && !closed } }
    }
    fun close() {
        if (closed) return
        closed = true
        active = false
        lifetime.cancel()
    }
}
