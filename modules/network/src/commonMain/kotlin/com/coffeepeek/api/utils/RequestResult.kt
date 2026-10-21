package com.coffeepeek.api.utils

import kotlinx.coroutines.CancellationException

suspend inline fun <T> requestResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
