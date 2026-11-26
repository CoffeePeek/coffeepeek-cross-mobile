package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.di.community.*
import com.coffeepeek.admin.di.homecheckin.*
import com.coffeepeek.admin.di.media.*
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityCardActions
import com.coffeepeek.feature.homecheckin.impl.*
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString

/** Retains the history bridge's detail/edit/save semantics while native UI owns presentation. */
class IosHomeCheckInRoute internal constructor(private val accounts: SessionReader, private val editors: HistoryHomeEditorFactory,
    private val language: LanguagePort, private val fetcher: ImageUrlFetcher, private val id: String, private val editOnLoad: Boolean,
) {
    private val host = CheckInHistoryHostViewModel(accounts)
    private val store = ViewModelStore().apply { put("native-home-route", host) }
    private val output = MutableStateFlow(IosHomeCheckInRouteState())
    private val media = IosCatalogueMedia(fetcher, Constants.BASE_URL)
    private var closed = false
    init {
        host.viewModelScope.launch { language.observe().collectLatest {
            val title = getString(Res.string.history_session_unavailable)
            val retry = getString(Res.string.history_session_retry)
            val back = getString(Res.string.history_session_back)
            output.update { it.copy(title = title, retry = retry, back = back) }
        } }
        host.viewModelScope.launch { host.session.collectLatest(::bind) }
    }
    fun current() = output.value
    fun observe(receive: (IosHomeCheckInRouteState) -> Unit) = IosHomeCheckInObservation(host.viewModelScope.launch { output.collect(receive) })
    fun retry() { if (!closed) host.retry() }
    fun back() {
        if (closed || !output.value.dismissAllowed) return
        output.value.editor?.requestClose() ?: output.value.detail?.perform("back") ?: Navigator.popBack()
    }
    fun requestEditorDismiss(editor: IosHomeCheckInEditor) { if (output.value.editor === editor) editor.requestClose() }
    fun dismissPhoto(photo: IosCheckInPhotoPresentation) {
        if (output.value.photo !== photo) return
        photo.close()
        output.update { it.copy(photo = null) }
    }
    fun close() {
        if (closed) return
        closed = true
        clearContent()
        store.clear()
    }
    private fun clearContent() {
        output.value.editor?.revoke()
        output.value.detail?.close()
        output.value.photo?.close()
        output.update { it.copy(detail = null, editor = null, photo = null, dismissAllowed = true) }
    }

    private suspend fun bind(session: HistoryHostSession) {
        clearContent()
        val failed = session.observationFailed || !session.lease.isCurrent() || !session.lease.isLoggedIn
        output.update { it.copy(failed = failed) }
        if (closed || failed) return
        val bridge = HistoryHomeBridge(session, editors)
        session.viewModelStore.put("native-home-bridge", bridge)
        bridge.open(id, editOnLoad && host.consumeInitialHomeId(id) != null)
        try { bridge.destination.collectLatest { destination ->
            clearContent()
            if (destination == null) {
                if (!closed && session.lease.isCurrent()) Navigator.popBack()
            } else if (destination.editing) bindEditor(session, bridge, destination)
            else bindDetail(session, bridge, destination)
        } } finally { clearContent() }
    }

    private suspend fun bindEditor(session: HistoryHostSession, bridge: HistoryHomeBridge, destination: HistoryHomeDestination): Nothing = coroutineScope {
        val core = (destination.entries.editor as NativeHomeCheckInEntry).createNative(destination.id, session.generation, null)
        val editor = ownedNativeHomeEditor(core, language, media, accounts)
        output.update { it.copy(editor = editor) }
        try {
            launch { core.state.collect {
                bridge.setDismissAllowed(destination, it.status.dismissAllowed)
                output.update { state -> state.copy(dismissAllowed = it.status.dismissAllowed) }
            } }
            launch { core.events.collect { event -> if (core.isCurrent() && bridge.destination.value === destination) when (event.kind) {
                "close" -> bridge.dismiss(destination)
                "saved" -> event.preparationId?.let { bridge.saved(destination, it, event.visibilityUnconfirmed) }
            } } }
            awaitCancellation()
        } finally {
            editor.revoke()
            coroutineContext.cancelChildren()
        }
    }

    private suspend fun bindDetail(session: HistoryHostSession, bridge: HistoryHomeBridge, destination: HistoryHomeDestination) = coroutineScope {
        val binding = detailBinding(session, bridge, destination)
        var detail: IosCheckInDetail? = null
        try {
            val initial = binding.state.filterNotNull().first()
            val source = IosCheckInDetail(initial.data.nativeDetail(initial.actions), CommunityCardActions(initial.actions.actions, initial.actions.onPhoto), media, accounts)
            detail = source
            source.bindOwner(null, binding::visibility, binding::close)
            output.update { it.copy(detail = source) }
            observeDetail(binding, source, bridge, destination)
        } finally {
            detail?.close()
            binding.close()
            coroutineContext.cancelChildren()
        }
    }

    private fun CoroutineScope.detailBinding(session: HistoryHostSession, bridge: HistoryHomeBridge,
        destination: HistoryHomeDestination,
    ): NativeHomeDetailPresentationSession {
        val core = (destination.entries.detail as NativeHomeDetailEntry).createNativeDetail(checkNotNull(destination.id), session.generation)
        var notice: String? = null
        val languages = language.observe().mapLatest {
            notice = if (destination.visibilityUnconfirmed) getString(Res.string.history_home_visibility_unconfirmed) else null
            it.languageTag
        }.shareIn(this, SharingStarted.Eagerly, replay = 1)
        val navigation = NativeHomeDetailNavigation({ bridge.dismiss(destination) }, { bridge.edit(destination, it) },
            { bridge.deleted(destination, it) }, { urls, originals, index ->
                bridge.openPhoto(destination, urls, originals, index, ::openPhoto)
            })
        return NativeHomeDetailPresentationSession(core, languages, ::formatNativeHomeDate, appHomeCheckInDates().formatMethod, { notice }, navigation)
    }

    private suspend fun observeDetail(binding: NativeHomeDetailPresentationSession, source: IosCheckInDetail,
        bridge: HistoryHomeBridge, destination: HistoryHomeDestination,
    ) {
        binding.state.filterNotNull().collect { snapshot ->
            source.update(snapshot.data.nativeDetail(snapshot.actions), CommunityCardActions(snapshot.actions.actions, snapshot.actions.onPhoto))
            bridge.setDismissAllowed(destination, snapshot.data.dismissAllowed)
            output.update { it.copy(dismissAllowed = snapshot.data.dismissAllowed, navigationTitle = snapshot.data.title,
                detailBack = snapshot.data.backLabel) }
        }
    }

    private fun openPhoto(urls: List<String>, originals: List<String>, index: Int) {
        if (closed || output.value.photo != null) return
        var source: IosCheckInPhotoPresentation? = null
        source = photoPresentation(CoffeePeekPhotoViewerImages(urls, originals), index, accounts, fetcher, language) { source?.let(::dismissPhoto) }
        output.update { it.copy(photo = source) }
    }
}

private fun formatNativeHomeDate(civil: String, instant: String): String = listOf(civil.split('-').reversed().joinToString("."),
    utcIsoToLocalDateTime(instant).substringAfterLast(' ', "")).filter(String::isNotBlank).joinToString(" · ")

data class IosHomeCheckInRouteState(val detail: IosCheckInDetail? = null, val editor: IosHomeCheckInEditor? = null,
    val photo: IosCheckInPhotoPresentation? = null, val failed: Boolean = false, val dismissAllowed: Boolean = true,
    val title: String = "", val retry: String = "", val back: String = "", val navigationTitle: String = "", val detailBack: String = "")
