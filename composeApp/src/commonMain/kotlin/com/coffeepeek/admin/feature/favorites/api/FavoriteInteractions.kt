package com.coffeepeek.admin.feature.favorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Presentation entry point. Keys remain in memory and are never analytics properties. */
internal interface FavoriteInteractions {
    fun change(key: String, roaster: Boolean, desired: Boolean, action: suspend () -> Result<Unit>, completed: suspend (Result<Unit>) -> Unit = {})
    companion object {
        var current: FavoriteInteractions? = null
        /** Analytics setup is optional; an unavailable integration must never suppress the action. */
        fun change(scope: CoroutineScope, key: String, roaster: Boolean, desired: Boolean,
            action: suspend () -> Result<Unit>, completed: suspend (Result<Unit>) -> Unit = {}) {
            val interactions = current
            if (interactions != null) interactions.change(key, roaster, desired, action, completed)
            else scope.launch { completed(action()) }
        }
    }
}
