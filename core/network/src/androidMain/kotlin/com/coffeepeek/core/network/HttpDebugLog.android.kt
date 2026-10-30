package com.coffeepeek.core.network

import android.util.Log

private const val TAG = "CoffeePeek-HTTP"

internal actual fun httpDebugLog(message: String) {
    Log.d(TAG, message)
}
