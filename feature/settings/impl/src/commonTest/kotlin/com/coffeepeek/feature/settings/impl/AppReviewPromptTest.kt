package com.coffeepeek.feature.settings.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AppReviewPromptTest {
    @Test
    fun leavingDuringTheDelayDoesNotConsumeTheAttempt() = runTest {
        val store = EligibleReviewStore()
        val platform = store.platform()
        val pending = launch { platform.promptWhenEligible { true } }
        advanceTimeBy(4.seconds)
        assertEquals(0, store.requests)
        pending.cancelAndJoin()
        platform.promptWhenEligible { true }
        assertEquals(1, store.requests)
    }

    @Test
    fun losingFocusWhilePlayPreparesPreventsTheRequest() = runTest {
        val store = EligibleReviewStore()
        var active = true
        val platform = store.platform(beforeClaim = { active = false })
        platform.promptWhenEligible { active }
        assertEquals(0, store.requests)
        store.platform().promptWhenEligible { true }
        assertEquals(1, store.requests)
    }

    @Test
    fun overlappingHostsAndRecreatedHostsShareTheCooldown() = runTest {
        val store = EligibleReviewStore()
        launch { store.platform().promptWhenEligible { true } }
        launch { store.platform().promptWhenEligible { true } }
        advanceTimeBy(5.seconds)
        runCurrent()
        store.platform().promptWhenEligible { true }
        assertEquals(1, store.requests)
    }

    private class EligibleReviewStore {
        private val day = 1.days.inWholeSeconds
        private val firstDay = 20_000L
        private var stored = "${firstDay * day},${firstDay + 2},3,0"
        var requests = 0
            private set

        fun platform(beforeClaim: () -> Unit = {}) = AppReviewPlatform(
            read = { stored },
            save = { stored = it },
            canPresent = { true },
            request = { claim ->
                beforeClaim()
                if (claim()) requests++
            },
            now = { (firstDay + 3) * day },
        )
    }
}
