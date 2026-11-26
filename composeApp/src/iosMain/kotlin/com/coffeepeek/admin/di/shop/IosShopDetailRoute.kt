package com.coffeepeek.admin.di.shop

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.impl.NativeShopLinkedCheckIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** The route survives a browsing-lease retry; each private source has a shorter lifetime. */
class IosShopDetailRoute internal constructor(factory: IosNativeShopDetailFactory, accounts: SessionReader,
    visibility: AuthorVisibility, language: LanguagePort, shopId: String, linkedCheckIn: Boolean,
) {
    private val host = ShopBrowsingHostViewModel(accounts, visibility)
    private val store = ViewModelStore().apply { put("native-shop-route", host) }
    private val mutable = MutableStateFlow(IosShopDetailRouteState())
    private var closed = false
    private val linkedRequest = NativeShopLinkedCheckIn(linkedCheckIn)
    init {
        host.viewModelScope.launch { language.observe().collectLatest {
            val title = getString(Res.string.shop_session_unavailable)
            val retry = getString(Res.string.shop_session_retry)
            val back = getString(Res.string.navigation_back)
            mutable.update { it.copy(title = title, retry = retry, back = back) }
        } }
        host.viewModelScope.launch { host.session.collectLatest { session ->
            mutable.update { it.copy(source = null, failed = session.observationFailed) }
            if (!closed && !session.observationFailed && session.lease.isCurrent()) factory.bind(this@IosShopDetailRoute, session, shopId, linkedRequest)
        } }
    }
    fun current() = mutable.value
    fun observe(receive: (IosShopDetailRouteState) -> Unit) = IosShopCheckInObservation(host.viewModelScope.launch { mutable.collect(receive) })
    fun back() { if (!closed) Navigator.popBack() }
    fun retry() { if (!closed) host.retry() }
    internal fun update(source: IosShopDetail?) { mutable.update { it.copy(source = source) } }
    fun close() {
        if (closed) return
        closed = true
        mutable.value.source?.revoke()
        update(null)
        store.clear()
    }
}

data class IosShopDetailRouteState(val source: IosShopDetail? = null, val failed: Boolean = false,
    val title: String = "", val retry: String = "", val back: String = "")
