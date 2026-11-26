package com.coffeepeek.feature.ownership.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.ownership.domain.model.BusinessKind
import com.coffeepeek.feature.ownership.domain.model.BusinessPhoto
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlin.uuid.Uuid

data class NativeOwnershipPhoto(val fileName: String, val contentType: String, val bytes: ByteArray)

fun createIosOwnershipClaimSession(entry: BusinessOwnershipEntry, kind: String, slug: String,
    photos: (Int, (List<NativeOwnershipPhoto>) -> Unit) -> Unit,
    onClose: () -> Unit, onLogin: () -> Unit): IosOwnershipSession {
    return NativeOwnershipSession(entry as OwnershipEntry, false, true, BusinessKind.valueOf(kind), slug, null,
        photos, onClose, onLogin, {}).session
}

fun createIosOwnershipSession(entry: BusinessOwnershipEntry, tab: Boolean,
    initialKind: String?, initialSlug: String?, initialId: String?,
    photos: (Int, (List<NativeOwnershipPhoto>) -> Unit) -> Unit,
    onClose: () -> Unit, onLogin: () -> Unit, onDispose: () -> Unit = {}): IosOwnershipSession {
    return NativeOwnershipSession(entry as OwnershipEntry, tab, false,
        BusinessKind.entries.find { it.name == initialKind }, initialSlug, initialId, photos, onClose, onLogin, onDispose).session
}

/** Native route lifetime; shared model remains the only writer and account observer. */
private class NativeOwnershipSession(private val entry: OwnershipEntry, tab: Boolean, claim: Boolean,
    kind: BusinessKind?, slug: String?, id: String?,
    private val photos: (Int, (List<NativeOwnershipPhoto>) -> Unit) -> Unit,
    private val onClose: () -> Unit, private val onLogin: () -> Unit, private val onDispose: () -> Unit) {
    private val model = entry.nativeModel(tab, kind, slug, id)
    private val store = ViewModelStore().apply { put("native-claim", model) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var closed = false
    private var completed = false
    val session = entry.nativeSession(model, tab, claim, ::choosePhotos, ::close)

    init {
        scope.launch { model.events.collect(::event) }
        if (claim) scope.launch { model.state.collect { if (it.generation > 1) complete(false) } }
    }

    private fun event(event: OwnershipEvent) {
        when (event) {
            OwnershipEvent.ProfileSaved -> entry.nativeProfileSaved()
            OwnershipEvent.Close -> complete(false)
            OwnershipEvent.Login -> complete(true)
        }
    }

    private fun choosePhotos() {
        val state = model.state.value
        val evidence = state.destination == OwnershipDestination.Submit
        val count = if (evidence) state.submission.photos.size else state.draft?.photos.orEmpty().size
        val maximum = (if (evidence) 3 else 10) - count
        if (closed || maximum <= 0 || !session.isCurrent(state.generation)) return
        photos(maximum) { selected ->
            if (!closed && session.isCurrent(state.generation)) {
                val values = selected.take(maximum).map { BusinessPhoto(Uuid.random().toString(),
                    fileName = it.fileName, contentType = it.contentType, size = it.bytes.size.toLong(), bytes = it.bytes.copyOf()) }
                model.onAction(if (evidence) OwnershipAction.AddEvidence(state.generation, values)
                    else OwnershipAction.AddPhotos(state.generation, values))
            }
        }
    }

    private fun complete(login: Boolean) {
        if (closed || completed) return
        completed = true
        if (login) onLogin() else onClose()
    }

    private fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        store.clear()
        onDispose()
    }
}
