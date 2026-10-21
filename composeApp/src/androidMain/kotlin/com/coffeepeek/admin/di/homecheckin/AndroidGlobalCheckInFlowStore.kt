package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Parent lifecycle retains the flow and editor together; final sheet removal clears both. */
internal class AndroidGlobalCheckInFlowStore : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    fun dispose(changingConfigurations: Boolean) {
        if (!changingConfigurations) viewModelStore.clear()
    }

    override fun onCleared() { viewModelStore.clear() }
}
