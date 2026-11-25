package com.coffeepeek.admin.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal data class NativeRootNavigationCommand(val id: Long, val generation: Long,
    val stacks: NativeRootStacks, val ack: CompletableDeferred<Unit> = CompletableDeferred())

/** A UIKit transition already started must settle before a newer intent can plan its next stack.
 * Root close/reset explicitly cancels ack, so revocation never waits for an obsolete controller. */
internal suspend fun NativeRootNavigationCommand.awaitShown() = withContext(NonCancellable) { ack.await() }
