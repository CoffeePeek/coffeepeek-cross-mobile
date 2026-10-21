package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RetainedViewModelStoreTest {
    class Flow : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    @Test fun flowSurvivesUntilReleasedAndTheNextFlowStartsFresh() {
        val store = AndroidGlobalCheckInFlowStore()
        val provider = ViewModelProvider.create(store, ViewModelProvider.NewInstanceFactory())
        val first = provider["flow", Flow::class]
        assertSame(first, provider["flow", Flow::class])
        assertFalse(first.cleared)
        store.dispose(changingConfigurations = false)
        assertTrue(first.cleared)
        assertFalse(first === provider["flow", Flow::class])
    }
}
