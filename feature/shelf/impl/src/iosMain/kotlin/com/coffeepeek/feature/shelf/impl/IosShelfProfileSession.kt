package com.coffeepeek.feature.shelf.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.ui.PublicShelfProfileViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfProfileVisibilityViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import com.coffeepeek.feature.shelf.impl.ui.compose.component.publicShelfPhotoFits
import kotlinx.coroutines.launch

fun createIosShelfProfileSession(repository: ShelfProfileRepository, slug: String, own: Boolean) =
    IosShelfProfileSession(PublicShelfProfileViewModel(repository, slug, own), repository)
fun createIosShelfVisibilitySession(repository: ShelfProfileRepository) =
    IosShelfVisibilitySession(ShelfProfileVisibilityViewModel(repository))

class IosShelfProfileSession internal constructor(private val model: PublicShelfProfileViewModel,
    private val repository: ShelfProfileRepository) {
    private val store = ViewModelStore().apply { put("shelf-profile", model) }
    fun current() = ShelfNativePublicSnapshot(model.state.value)
    fun observe(receive: () -> Unit) = model.viewModelScope.launch { model.state.collect { receive() } }
    fun events(receive: (String) -> Unit) = model.viewModelScope.launch {
        model.events.collect { if (it is PublicShelfProfileEvent.OpenCoffee) receive(it.slug) }
    }
    fun refresh() = model.onAction(PublicShelfProfileAction.Refresh)
    fun open(id: String) = model.onAction(PublicShelfProfileAction.OpenCoffee(id))
    fun photoUrl(id: String) = model.state.value.shelf?.items?.firstOrNull { it.id == id }?.photoUrl
    suspend fun photo(id: String): ByteArray? {
        val revision = model.state.value.revision
        val url = photoUrl(id) ?: return null
        val result = repository.photo(url)
        result.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
        val bytes = result.getOrNull() ?: return null
        if (revision == model.state.value.revision && photoUrl(id) == url && publicShelfPhotoFits(bytes)) return bytes
        bytes.fill(0)
        return null
    }
    fun close() = store.clear()
}

class ShelfNativePublicSnapshot internal constructor(state: PublicShelfProfileState) {
    val loading = state.loading
    val failed = state.failed
    val revision = state.revision
    val visible = state.shelf != null
    val items = state.shelf?.items.orEmpty().map { ShelfNativePublicItem(it.id, it.name, it.roasterName, it.photoUrl != null, it.coffeeSlug != null) }
}
class ShelfNativePublicItem internal constructor(val id: String, val title: String, val roaster: String?, val hasPhoto: Boolean, val hasCoffee: Boolean)

class IosShelfVisibilitySession internal constructor(private val model: ShelfProfileVisibilityViewModel) {
    private val store = ViewModelStore().apply { put("shelf-visibility", model) }
    fun current() = ShelfNativeVisibilitySnapshot(model.state.value)
    fun observe(receive: () -> Unit) = model.viewModelScope.launch { model.state.collect { receive() } }
    fun set(value: Boolean) = model.onAction(ShelfProfileVisibilityAction.SetVisible(value))
    fun refresh() = model.onAction(ShelfProfileVisibilityAction.Refresh)
    fun close() = store.clear()
}
class ShelfNativeVisibilitySnapshot internal constructor(state: ShelfProfileVisibilityState) {
    val visible = state.visible
    val enabled = state.signedIn && state.loaded && !state.loading && !state.busy
    val loading = state.loading || state.busy
    val failed = state.error != null
}
