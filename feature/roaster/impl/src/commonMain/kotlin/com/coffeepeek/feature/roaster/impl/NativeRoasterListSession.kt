package com.coffeepeek.feature.roaster.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterListAnalytics
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.ui.RoasterListViewModel
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter

typealias NativeRoasterDiscoveryContent = @Composable (NativeRoasterListSession, NativeRoasterDiscoveryHost) -> Boolean

class NativeRoasterDiscoveryHost(
    val query: String, val selectedRoasters: Set<String>, val favoritesOnly: Boolean, val search: Boolean,
    val onOpen: (String) -> Unit, val onAdd: () -> Unit, val onQuery: (String) -> Unit,
    val onCancel: () -> Unit, val onFavoriteFeedback: () -> Unit, val modifier: Modifier, val bottomClearance: Float,
)

fun createNativeRoasterListSession(repository: RoasterRepository,
    favorites: RoasterFavoritesPort = RoasterFavoritesPort.None,
    analytics: RoasterListAnalytics = RoasterListAnalytics.None): NativeRoasterListSession {
    val model = RoasterListViewModel(repository, favorites, analytics)
    val store = ViewModelStore().apply { put("native-roaster-list", model) }
    return NativeRoasterListSession(model, store::clear)
}

class NativeRoasterListSession internal constructor(private val model: RoasterListViewModel,
    private val dispose: () -> Unit = {}) {
    private var closed = false
    val state = model.state.map { current() }
    val opened = model.events.filter { !closed }.map { (it as RoasterListEvent.OpenRoaster).slug }
    fun current() = if (closed) NativeRoasterListState(emptyList(), emptyList(), false, false, "", null, null)
        else model.state.value.native()
    fun criteria(query: String, selected: Set<String>, favorites: Boolean) {
        dispatch(RoasterListAction.QueryChanged(query))
        dispatch(RoasterListAction.SelectedRoastersChanged(selected))
        dispatch(RoasterListAction.FavoritesOnlyChanged(favorites))
    }
    fun retry() = dispatch(RoasterListAction.Refresh)
    fun hasResults(query: String, selected: Set<String>, favorites: Boolean): Boolean? =
        if (closed) false else model.state.value.hasResults(query, selected, favorites)
    fun open(slug: String) = dispatch(RoasterListAction.OpenRoaster(slug))
    fun favorite(slug: String) = dispatch(RoasterListAction.ToggleFavorite(slug))
    fun appeared(slug: String) = dispatch(RoasterListAction.LoadDetails(slug))
    fun tag(slug: String) = dispatch(RoasterListAction.ToggleTag(slug))
    fun clearTags() = dispatch(RoasterListAction.ClearTags)
    fun dismissMessage() = dispatch(RoasterListAction.DismissMessage)
    fun close() { if (!closed) { closed = true; dispose() } }
    private fun dispatch(action: RoasterListAction) { if (!closed) model.onAction(action) }
}

data class NativeRoasterCard(
    val slug: String?, val name: String, val photo: String?, val about: String?,
    val shops: Int, val products: Int, val favorite: Boolean,
)
data class NativeRoasterTag(val id: String, val name: String, val selected: Boolean)
data class NativeRoasterListState(
    val cards: List<NativeRoasterCard>, val tags: List<NativeRoasterTag>, val loading: Boolean,
    val failed: Boolean, val query: String, val messageKey: String?, val message: String?,
)

private fun RoasterListState.native() = NativeRoasterListState(
    visibleItems.map { NativeRoasterCard(it.slug, it.summary.name, it.summary.photoUrl ?: it.details?.photos?.firstOrNull()?.url,
        it.details?.about, it.summary.coffeeShopsCount, it.summary.coffeeProductsCount, it.favoriteKey in favoriteKeys) },
    availableTags.map { NativeRoasterTag(it.slug, it.name, it.slug in selectedTags) },
    isLoading, loadFailed, query, message.messageKey(), (message as? RoasterMessage.FromStore)?.text,
)
