package com.coffeepeek.admin.di.shop

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopDetailContext
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString

/** Picker-to-editor route, including native loading and recoverable session/detail errors. */
class IosShopCheckInRoute internal constructor(accounts: SessionReader, details: ShopDetailsRepository,
    forms: SessionShopCheckInEntry, language: LanguagePort, id: String, private val shopSelectionIncluded: Boolean,
) {
    private val host = ShopBrowsingHostViewModel(accounts)
    private val store = ViewModelStore().apply { put("native-shop-editor-route", host) }
    private val state = MutableStateFlow(IosShopCheckInRouteState())
    private var closed = false
    private var ended = false
    init {
        host.viewModelScope.launch { language.observe().collectLatest {
            val title = getString(Res.string.check_in_shop_unavailable)
            val retry = getString(Res.string.shop_session_retry)
            val back = getString(Res.string.navigation_back)
            state.update { it.copy(title = title, retry = retry, back = back) }
        } }
        host.viewModelScope.launch { host.session.collectLatest { session ->
            clear()
            state.update { it.copy(failed = session.observationFailed) }
            if (!closed && !session.observationFailed && session.lease.isCurrent()) bind(session, accounts, details, forms, language, id)
        } }
    }
    fun current() = state.value
    fun observe(receive: (IosShopCheckInRouteState) -> Unit) = IosShopCheckInObservation(host.viewModelScope.launch { state.collect(receive) })
    fun back() {
        if (closed || ended) return
        state.value.editor?.dismiss() ?: leaveLoadingScreen()
    }
    fun retry() { if (!closed && !ended) host.retry() }
    fun close() {
        if (closed) return
        closed = true
        clear()
        store.clear()
    }
    private fun clear() {
        state.value.editor?.revoke()
        state.update { it.copy(editor = null) }
    }
    private fun revoked() {
        clear()
        state.update { it.copy(failed = true) }
    }

    private suspend fun bind(session: ShopBrowsingHostSession, accounts: SessionReader, details: ShopDetailsRepository,
        forms: SessionShopCheckInEntry, language: LanguagePort, id: String,
    ) {
        val result = session.lease.details(details).getDetails(id)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (!live(session) || ended) return
        val overview = result.getOrNull()?.overview
        if (overview == null) {
            state.update { it.copy(failed = true) }
            return
        }
        val context = ShopDetailContext(id, overview.id, overview.title, overview.address)
        val callbacks = callbacks(session)
        val editor = nativeShopCheckInForm(forms, context, language, accounts, callbacks, shopSelectionIncluded, ::revoked)
        if (editor == null) {
            revoked()
            return
        }
        state.update { it.copy(editor = editor) }
        try { awaitCancellation() } finally { editor.revoke() }
    }

    private fun callbacks(session: ShopBrowsingHostSession) = ShopCheckInNativeCallbacks(
        { finish(session, Navigator::popBack) },
        { finish(session) { Navigator.popThenNavigate(Navigator.Screen.CreateCheckIn) } }, {},
        { finish(session) { Navigator.popThenSelectTab(Navigator.Screen.CommunityTab) } },
        { finish(session) { Navigator.openProfileCheckIns(popCurrent = true) } },
    )

    private fun leaveLoadingScreen() {
        ended = true
        if (shopSelectionIncluded) Navigator.popThenNavigate(Navigator.Screen.CreateCheckIn) else Navigator.popBack()
    }
    private fun finish(session: ShopBrowsingHostSession, action: () -> Unit) {
        if (ended || !live(session)) return
        ended = true
        clear()
        action()
    }
    private fun live(session: ShopBrowsingHostSession) = !closed && session.lease.isCurrent()
}

data class IosShopCheckInRouteState(val editor: IosShopCheckInEditor? = null, val failed: Boolean = false,
    val title: String = "", val retry: String = "", val back: String = "")
