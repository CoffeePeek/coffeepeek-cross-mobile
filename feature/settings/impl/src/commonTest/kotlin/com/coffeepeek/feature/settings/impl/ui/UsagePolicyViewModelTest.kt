package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.UsageConsentPort
import com.coffeepeek.feature.settings.domain.UsageConsentState
import com.coffeepeek.feature.settings.impl.ui.model.UsagePolicyAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UsagePolicyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private class FakeConsent(initial: UsageConsentState) : UsageConsentPort {
        val state = MutableStateFlow(initial)
        val calls = mutableListOf<Boolean>()
        override fun observe(): Flow<UsageConsentState> = state
        override suspend fun set(accepted: Boolean) { calls += accepted; state.value = state.value.copy(accepted = accepted) }
    }

    private fun model(port: FakeConsent) = UsagePolicyViewModel(port).also { store.put("policy", it) }

    @Test fun theSwitchIsHiddenWithoutASignedInAnalyticsIdentityAndTogglingDoesNothing() = runTest(dispatcher) {
        val port = FakeConsent(UsageConsentState.Unavailable); val vm = model(port); runCurrent()
        assertEquals(UsageConsentState.Unavailable, vm.state.value.consent)
        vm.onAction(UsagePolicyAction.SetConsent(true)); runCurrent()
        assertEquals(emptyList(), port.calls)
    }

    @Test fun togglingAppliesTheChoiceThroughThePortAndFollowsTheAppState() = runTest(dispatcher) {
        val port = FakeConsent(UsageConsentState(available = true, accepted = false)); val vm = model(port); runCurrent()
        vm.onAction(UsagePolicyAction.SetConsent(true)); runCurrent()
        assertEquals(listOf(true), port.calls)
        assertEquals(UsageConsentState(available = true, accepted = true), vm.state.value.consent)
        vm.onAction(UsagePolicyAction.SetConsent(false)); runCurrent()
        assertEquals(listOf(true, false), port.calls)
        assertEquals(false, vm.state.value.consent.accepted)
    }

    @Test fun theSwitchDisappearsWhenTheUserSignsOut() = runTest(dispatcher) {
        val port = FakeConsent(UsageConsentState(available = true, accepted = true)); val vm = model(port); runCurrent()
        port.state.value = UsageConsentState.Unavailable; runCurrent()
        assertEquals(false, vm.state.value.consent.available)
    }
}
