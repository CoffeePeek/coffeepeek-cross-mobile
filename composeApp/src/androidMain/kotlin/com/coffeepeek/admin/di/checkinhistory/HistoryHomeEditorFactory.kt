package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.feature.homecheckin.domain.*

/** Temporary Android composition seam; Home still owns its existing editor and writer. */
internal fun interface HistoryHomeEditorFactory {
    fun create(lease: AndroidCheckInHistorySession, onMutation: () -> Unit, onDeleted: () -> Unit): HomeCheckInViewModel
}

internal fun historyHomeEditorFactory(repository: HomeCheckInRepository, picker: CoffeePicker,
    methods: BrewMethodCatalog,
) = HistoryHomeEditorFactory { lease, onMutation, onDeleted ->
    val editorRepository = object : HomeCheckInRepository {
        // This VM is used ONLY for its editor/detail. The new MVI owns the actual timeline.
        override suspend fun history(page: Int, pageSize: Int, from: String?, to: String?) =
            Result.success(HomeCheckInPage(emptyList(), 0, page, pageSize))
        override suspend fun calendar(from: String, to: String) = Result.success(emptyList<HomeCalendarDay>())
        override suspend fun detail(id: String) = lease.withCurrentAccount {
            repository.detail(id).mapCatching { check(it.id == id); it }
        }
        override suspend fun create(input: HomeCheckInInput) =
            lease.withCurrentAccount { repository.create(input) }.also { if (it.isSuccess) onMutation() }
        override suspend fun update(id: String, input: HomeCheckInInput) =
            lease.withCurrentAccount { repository.update(id, input) }.also { if (it.isSuccess) onMutation() }
        override suspend fun delete(id: String) =
            lease.withCurrentAccount { repository.delete(id) }.also {
                if (it.isSuccess) { onMutation(); onDeleted() }
            }
        override suspend fun photo(url: String) = lease.withCurrentAccount { repository.photo(url) }
        override suspend fun beanStats() = lease.withCurrentAccount { repository.beanStats() }
        override suspend fun methodStats() = lease.withCurrentAccount { repository.methodStats() }
    }
    HomeCheckInViewModel(editorRepository, object : CoffeePicker {
        override suspend fun search(query: String) = lease.withCurrentAccount { picker.search(query) }
    }, BrewMethodCatalog { lease.withCurrentAccount { methods.load() } })
}
