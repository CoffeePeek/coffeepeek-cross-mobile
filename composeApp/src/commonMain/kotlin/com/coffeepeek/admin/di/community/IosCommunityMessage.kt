package com.coffeepeek.admin.di.community

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Queue identity stays shared; native presentation owns placement and accessible timeout delivery. */
class IosCommunityMessage internal constructor(
    val text: String,
    val actionLabel: String?,
    val dismissLabel: String,
    private val isCurrent: () -> Boolean,
    private val onDismiss: () -> Unit,
    private val onAction: () -> Unit,
) {
    private var closed = false
    private val mutableHeight = MutableStateFlow(64.0)
    internal val height = mutableHeight.asStateFlow()

    fun dismiss() { if (active()) onDismiss() }
    fun performAction() { if (active() && actionLabel != null) onAction() }
    fun heightChanged(height: Double) {
        if (!closed && height.isFinite() && height > 0) mutableHeight.value = height
    }
    internal fun close() { closed = true }
    private fun active() = !closed && isCurrent()
}
