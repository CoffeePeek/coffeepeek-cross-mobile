@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.admin.di.discovery

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.shop.CheckInFlowHandoff
import com.coffeepeek.admin.di.shop.ShopBrowsingHostSession
import com.coffeepeek.admin.di.shop.ShopBrowsingHostViewModel
import com.coffeepeek.admin.location.IosLocationPermission
import com.coffeepeek.admin.location.openAppLocationSettings
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.discovery.impl.ui.*
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/** A navigation-owned picker. Replacing a lease disposes its VMs, media and permissions together. */
class IosCheckInShopPicker internal constructor(private val factory: NativeCheckInShopPickerSessionFactory,
    private val accounts: SessionReader, private val language: LanguagePort, private val media: IosCatalogueMedia,
) {
    private val host = ShopBrowsingHostViewModel(accounts)
    private val store = ViewModelStore().apply { put("native-check-in-picker", host) }
    private val identity = accounts.discoveryAccount()
    private val mutableState = MutableStateFlow(IosCheckInShopPickerState())
    private var closed = false
    private val navigation = CheckInShopPickerNavigation(::isCurrent, Navigator::popThenNavigate, Navigator::popBack)

    init {
        observeLabels()
        observeSession()
        val tracker = CheckInFlowHandoff.tracker?.takeIf { it.isFresh }
            ?: CheckInTracker(CheckInKind.Visit, CheckInSource.Direct).also { CheckInFlowHandoff.tracker = it }
        tracker.step(CheckInStep.Shop)
    }

    fun currentState() = mutableState.value
    fun observe(receive: (IosCheckInShopPickerState) -> Unit) =
        IosDiscoveryObservation(host.viewModelScope.launch { mutableState.collect(receive) })
    fun retry() { if (isCurrent()) host.retry() }
    fun back() = navigation.close(back = true)
    fun dismiss() = navigation.close(back = false)
    fun close() {
        if (closed) return
        closed = true
        navigation.dispose()
        mutableState.value.discovery?.close()
        mutableState.value = mutableState.value.copy(discovery = null)
        store.clear()
    }
    private fun isCurrent() = !closed && identity == accounts.discoveryAccount()

    private fun observeLabels() {
        host.viewModelScope.launch { language.observe().collectLatest {
            val environment = getSystemResourceEnvironment()
            val title = nativeDiscoveryPickerTitle()
            val stepLabel = nativeDiscoveryPickerStepLabel()
            val failure = getString(environment, Res.string.shop_session_unavailable)
            val retry = getString(environment, Res.string.shop_session_retry)
            val close = getString(environment, Res.string.check_in_create_close)
            mutableState.update { it.copy(title = title, failure = failure, retry = retry, close = close, stepLabel = stepLabel) }
        } }
    }

    private fun observeSession() {
        host.viewModelScope.launch { host.session.collectLatest { session ->
            mutableState.update { it.copy(discovery = null, failed = session.observationFailed) }
            if (!isCurrent() || session.observationFailed || !session.lease.isCurrent()) return@collectLatest
            val history = CheckInHistorySession(accounts)
            val binding = NativeDiscoveryIosSession(factory.createNativeSession(session, history),
                language.observe().map { it.languageTag })
            val source = source(binding)
            mutableState.update { it.copy(discovery = source) }
            try { observeBinding(session, binding, source) } finally {
                source.close()
                history.revoke()
                mutableState.update { if (it.discovery === source) it.copy(discovery = null) else it }
            }
        } }
    }

    private fun source(binding: NativeDiscoveryIosSession): IosDiscovery {
        val core = binding.session
        return IosDiscovery(binding.presentations.value, media, accounts, initiallyReady = false,
            onVisibility = { core.presentation(it, 0f, false) }).also { it.bindDisposal(binding::close) }
    }

    private suspend fun observeBinding(session: ShopBrowsingHostSession, binding: NativeDiscoveryIosSession,
        source: IosDiscovery,
    ) = coroutineScope {
        val search = IosLocationPermission()
        val map = IosLocationPermission()
        launch { combine(binding.presentations, binding.ready) { data, ready -> data to ready }
            .collect { (data, ready) -> if (session.lease.isCurrent()) source.update(data, ready) } }
        launch { binding.session.runtime.collect {
            requestPermission(binding.session, search, NativeDiscoveryPermissionTarget.Search, it.searchPermission)
            requestPermission(binding.session, map, NativeDiscoveryPermissionTarget.Map, it.mapPermission)
        } }
        launch { source.screenVisible.collectLatest { shown ->
            if (shown) binding.session.events.collect { if (session.lease.isCurrent()) event(it) }
        } }
        try {
            binding.session.accountFailed.first { it }
            mutableState.update { if (it.discovery === source) it.copy(discovery = null, failed = true) else it }
        } finally {
            coroutineContext.cancelChildren()
            search.close()
            map.close()
        }
    }

    private fun requestPermission(core: NativeDiscoverySession, permission: IosLocationPermission,
        target: NativeDiscoveryPermissionTarget, request: Int,
    ) = permission.request(request,
        { core.permissionResult(target, request, NativeDiscoveryPermissionOutcome.Granted) },
        { core.permissionResult(target, request, NativeDiscoveryPermissionOutcome.Denied) },
        { core.permissionResult(target, request, NativeDiscoveryPermissionOutcome.PermanentlyDenied) })

    private fun event(event: NativeDiscoveryEvent) {
        if (!isCurrent()) return
        when (event) {
            is NativeDiscoveryEvent.OpenShop -> navigation.select(event.id)
            NativeDiscoveryEvent.Back -> back()
            NativeDiscoveryEvent.Close -> dismiss()
            NativeDiscoveryEvent.SignIn -> Navigator.navigate(Navigator.Screen.Auth)
            NativeDiscoveryEvent.OpenLocationSettings -> openAppLocationSettings()
            NativeDiscoveryEvent.FavoriteFeedback -> platformHaptic(HapticEvent.Selection)
            else -> Unit
        }
    }
}

data class IosCheckInShopPickerState(
    val discovery: IosDiscovery? = null, val failed: Boolean = false, val title: String = "",
    val failure: String = "", val retry: String = "", val close: String = "", val stepLabel: String = "",
)
