package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * An Activity-retained store for one create flow: its ViewModels survive configuration changes
 * (rotation, theme/font/locale, camera return) and are released only when the flow really ends.
 */
internal class RetainedViewModelStore : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    fun release() = viewModelStore.clear()

    override fun onCleared() = release()
}
