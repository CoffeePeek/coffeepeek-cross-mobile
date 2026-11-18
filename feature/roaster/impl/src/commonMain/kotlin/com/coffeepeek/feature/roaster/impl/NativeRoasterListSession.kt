package com.coffeepeek.feature.roaster.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.roaster.impl.ui.RoasterListViewModel
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import kotlinx.coroutines.flow.map

typealias NativeRoasterDiscoveryContent = @Composable (NativeRoasterListSession, NativeRoasterDiscoveryHost) -> Boolean

class NativeRoasterDiscoveryHost(
    val query: String, val selectedRoasters: Set<String>, val favoritesOnly: Boolean, val search: Boolean,
    val onOpen: (String) -> Unit, val onAdd: () -> Unit, val onQuery: (String) -> Unit,
    val onCancel: () -> Unit, val onFavoriteFeedback: () -> Unit, val modifier: Modifier, val bottomClearance: Float,
)

class NativeRoasterListSession internal constructor(private val model: RoasterListViewModel) {
    val state = model.state.map { it.native() }
    val opened = model.events.map { (it as RoasterListEvent.OpenRoaster).slug }
    fun current() = model.state.value.native()
    fun criteria(query: String, selected: Set<String>, favorites: Boolean) {
        model.onAction(RoasterListAction.QueryChanged(query))
        model.onAction(RoasterListAction.SelectedRoastersChanged(selected))
        model.onAction(RoasterListAction.FavoritesOnlyChanged(favorites))
    }
    fun retry() = model.onAction(RoasterListAction.Refresh)
    fun open(slug: String) = model.onAction(RoasterListAction.OpenRoaster(slug))
    fun favorite(slug: String) = model.onAction(RoasterListAction.ToggleFavorite(slug))
    fun appeared(slug: String) = model.onAction(RoasterListAction.LoadDetails(slug))
    fun tag(slug: String) = model.onAction(RoasterListAction.ToggleTag(slug))
    fun clearTags() = model.onAction(RoasterListAction.ClearTags)
    fun dismissMessage() = model.onAction(RoasterListAction.DismissMessage)
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
