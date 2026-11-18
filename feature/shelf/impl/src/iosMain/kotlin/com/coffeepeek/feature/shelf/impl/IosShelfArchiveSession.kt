package com.coffeepeek.feature.shelf.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.ShelfArchiveViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun createIosShelfFeature(repository: ShelfRepository, ports: ShelfPorts): IosShelfFeature =
    IosShelfFeature(createShelfFeature(repository, ports))

/** The native archive and the existing bag screen use the same feature handoff. */
class IosShelfFeature internal constructor(private val feature: ShelfApiImpl) {
    val entry: ShelfEntry get() = feature

    fun shelf() = IosShelfSession(feature.shelfModel())
    fun bag(id: String) = IosShelfBagSession(feature.bagModel(id))
    fun add() = IosShelfAddSession(feature.addModel())

    fun archive() = IosShelfArchiveSession(feature.archiveModel())
}

class IosShelfArchiveSession internal constructor(private val model: ShelfArchiveViewModel) {
    private val store = ViewModelStore().apply { put("shelf-archive", model) }

    fun current() = ShelfArchiveSnapshot(model.state.value)

    fun observe(receive: () -> Unit): Job = model.viewModelScope.launch {
        model.state.collect { receive() }
    }

    fun observeEvents(onBack: () -> Unit, onBag: (String) -> Unit): Job = model.viewModelScope.launch {
        model.events.collect { event ->
            when (event) {
                ShelfArchiveEvent.Back -> onBack()
                is ShelfArchiveEvent.OpenBag -> onBag(event.id)
            }
        }
    }

    fun query(value: String) = model.onAction(ShelfArchiveAction.QueryChanged(value))
    fun retry() = model.onAction(ShelfArchiveAction.Retry)
    fun more() = model.onAction(ShelfArchiveAction.LoadMore)
    fun back() = model.onAction(ShelfArchiveAction.Back)
    fun openBag(id: String) = model.onAction(ShelfArchiveAction.OpenBag(id))

    fun photo(id: String) = model.state.value.items.firstOrNull { it.id == id }?.bag?.photo?.url
    fun close() = store.clear()
}

class ShelfArchiveSnapshot internal constructor(state: ShelfArchiveState) {
    val currentYear = state.today.year
    val query = state.query
    val loading = state.loading
    val loadingMore = state.loadingMore
    val loadFailed = state.loadFailed
    val loadMoreFailed = state.loadMoreFailed
    val hasMore = state.hasMore
    val sections = state.sections.map { section ->
        val rows = section.items.map {
            ShelfArchiveRowSnapshot(it.id, it.bag.name, it.bag.roasterName, it.daysOnShelf(state.today), it.bag.rating, it.bag.photo != null)
        }
        ShelfArchiveSectionSnapshot(section.year, section.month, rows)
    }
}

class ShelfArchiveSectionSnapshot internal constructor(val year: Int, val month: Int, val items: List<ShelfArchiveRowSnapshot>)

class ShelfArchiveRowSnapshot internal constructor(
    val id: String, val title: String?, val roaster: String?, val days: Int?, val rating: Int?, val hasPhoto: Boolean,
)
