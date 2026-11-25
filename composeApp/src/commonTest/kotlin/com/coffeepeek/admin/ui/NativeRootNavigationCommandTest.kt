package com.coffeepeek.admin.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeRootNavigationCommandTest {
    @Test fun supersedingAnIntentWaitsForTheAlreadyStartedUIKitTransition() = runTest {
        val command = NativeRootNavigationCommand(1, 1, NativeRootNavigationModel(false).actual)
        val request = launch { command.awaitShown() }
        runCurrent()
        request.cancel()
        runCurrent()
        assertFalse(request.isCompleted)
        command.ack.complete(Unit)
        runCurrent()
        assertTrue(request.isCompleted)
        assertTrue(request.isCancelled)
    }
    @Test fun rootRevocationCancelsAnAcknowledgementWithoutWaitingForUIKit() = runTest {
        val command = NativeRootNavigationCommand(1, 1, NativeRootNavigationModel(false).actual)
        val request = launch { command.awaitShown() }
        runCurrent()
        command.ack.cancel()
        runCurrent()
        assertTrue(request.isCompleted)
        assertTrue(request.isCancelled)
    }
}
