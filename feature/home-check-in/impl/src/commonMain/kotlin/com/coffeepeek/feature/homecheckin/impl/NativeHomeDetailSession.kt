package com.coffeepeek.feature.homecheckin.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInDetailViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInDetailAction
import kotlinx.coroutines.flow.filter

/** Destination lifetime around the existing detail/delete state machine. */
class NativeHomeDetailSession(repository: HomePreparationRepository, private val canAccess: () -> Boolean,
    id: String, generation: Int,
) {
    private var closed = false
    private val model = HomeCheckInDetailViewModel(id, repository, generation, ::isCurrent)
    private val store = ViewModelStore().apply { put("native-home-detail", model) }
    internal val state = model.state
    internal val events = model.events.filter { !closed && model.canDeliver(it) }
    fun isCurrent() = !closed && canAccess()
    internal fun action(action: HomeCheckInDetailAction) { if (isCurrent()) model.onAction(action) }
    fun close() {
        if (closed) return
        closed = true
        store.clear()
    }
}

interface NativeHomeDetailEntry {
    fun createNativeDetail(id: String, generation: Int): NativeHomeDetailSession
}
