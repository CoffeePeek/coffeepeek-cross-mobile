@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import com.coffeepeek.feature.shelf.impl.IosShelfArchiveSession
import com.coffeepeek.feature.shelf.impl.ShelfArchiveRowSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import platform.UIKit.UIImage

/** A pushed archive is bound to one owner generation. Protected card URLs stay in Kotlin. */
class IosShelfArchive internal constructor(
    createSession: () -> IosShelfArchiveSession,
    private val account: SessionShelfAccount,
    private val media: IosCatalogueMedia,
    private val onBack: () -> Unit,
    private val onBag: (String) -> Unit,
    private val allowed: () -> Boolean = { true },
) {
    private val owner = account.visibilityScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val covers = CoroutineScope(SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Main)
    private val session = if (owner != null) createSession() else null
    private val revoked = MutableStateFlow(false)

    init {
        if (owner != null) scope.launch {
            account.visibilityScopes.first { it != owner }
            revoke()
        }
    }

    fun current(): IosShelfArchiveState {
        val state = if (isCurrent()) session?.current() else null
        return if (state == null) IosShelfArchiveState(signedIn = false)
        else IosShelfArchiveState(
            signedIn = true, query = state.query, loading = state.loading, loadingMore = state.loadingMore,
            loadFailed = state.loadFailed, loadMoreFailed = state.loadMoreFailed, hasMore = state.hasMore,
            sections = state.sections.map { section ->
                IosShelfArchiveSection(section.year.toString() + "-" + section.month, section.month,
                    section.year.takeIf { it != state.currentYear }, section.items.map(::nativeRow))
            },
        )
    }

    fun observe(receive: (IosShelfArchiveState) -> Unit): IosShelfArchiveObservation {
        val state = session?.observe { if (isCurrent()) receive(current()) }
        val events = session?.observeEvents(onBack) { id -> if (isCurrent()) onBag(id) }
        val replacement = scope.launch {
            revoked.first { it }
            receive(current())
        }
        return IosShelfArchiveObservation(state, events, replacement)
    }

    fun query(value: String) { if (isCurrent()) session?.query(value) }
    fun retry() { if (isCurrent()) session?.retry() }
    fun more() { if (isCurrent()) session?.more() }
    fun openBag(id: String) { if (isCurrent()) session?.openBag(id) }
    fun back() { if (isCurrent()) session?.back() else onBack() }

    fun loadCover(id: String, receive: (UIImage?) -> Unit): IosShelfArchiveObservation =
        IosShelfArchiveObservation(covers.launch {
            val url = if (isCurrent()) session?.photo(id) else null
            if (url == null) return@launch
            try {
                media.observeImage(url) { isCurrent() && session?.photo(id) == url }.collect {
                    receive(it.takeIf { isCurrent() && session?.photo(id) == url })
                }
            } catch (cancelled: CancellationException) {
                receive(null)
                throw cancelled
            } catch (_: Exception) { receive(null) }
        })

    fun close() {
        revoke()
        scope.cancel()
    }

    private fun revoke() {
        if (revoked.value) return
        revoked.value = true
        session?.close()
        covers.cancel()
    }

    private fun isCurrent() = !revoked.value && owner != null && account.visibilityScope == owner && allowed()
}

class IosShelfArchiveObservation internal constructor(private vararg val jobs: Job?) {
    fun cancel() = jobs.forEach { it?.cancel() }
}

data class IosShelfArchiveState(
    val signedIn: Boolean, val query: String = "", val loading: Boolean = false, val loadingMore: Boolean = false,
    val loadFailed: Boolean = false, val loadMoreFailed: Boolean = false, val hasMore: Boolean = false,
    val sections: List<IosShelfArchiveSection> = emptyList(),
)

data class IosShelfArchiveSection(val id: String, val month: Int, val earlierYear: Int?, val items: List<IosShelfArchiveRow>)

data class IosShelfArchiveRow(
    val id: String, val title: String?, val roaster: String?, val days: Int?, val daysKey: String?, val rating: Int?, val hasPhoto: Boolean,
)

private fun nativeRow(row: ShelfArchiveRowSnapshot) = IosShelfArchiveRow(
    row.id, row.title?.takeIf(String::isNotBlank), row.roaster, row.days,
    row.days?.let { coffeePeekPluralForm(it, "archive.days.one", "archive.days.few", "archive.days.many") },
    row.rating, row.hasPhoto,
)
