package com.coffeepeek.admin.di.checkinhistory

class IosHistoryFailure internal constructor(
    val message: String,
    val retryLabel: String,
    val backLabel: String,
    private val onRetry: () -> Unit,
    private val onBack: () -> Unit,
) {
    private var closed = false
    fun retry() { if (!closed) onRetry() }
    fun back() { if (!closed) onBack() }
    internal fun close() { closed = true }
}
