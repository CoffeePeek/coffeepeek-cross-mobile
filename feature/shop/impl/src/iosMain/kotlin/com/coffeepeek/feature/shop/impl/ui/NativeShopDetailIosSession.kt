package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.api.*
import com.coffeepeek.feature.shop.impl.NativeShopDetailSession
import com.coffeepeek.feature.shop.impl.NativeShopLinkedCheckIn
import com.coffeepeek.feature.shop.impl.toPublicEffect
import com.coffeepeek.feature.shop.impl.resources.*
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.UIKit.UIAccessibilityIsVoiceOverRunning

/** Resources and native intents adapt the same feature state; no feature composition is created. */
class NativeShopDetailIosSession(val core: NativeShopDetailSession, languages: Flow<String>,
    private val formatDate: (String) -> String, private val formatMenu: (String) -> String,
    private val distance: (Double, Double) -> String?, private val analytics: ShopAnalytics,
    private val effect: (ShopDetailEffect) -> Unit, private val linkedCheckIn: NativeShopLinkedCheckIn,
) {
    private val resources = MutableStateFlow<ShopDetailIosResources?>(null)
    private val revision = MutableStateFlow(0)
    private val message = MutableStateFlow<String?>(null)
    private val output = MutableStateFlow<ShopDetailIosPresentation?>(null)
    val state = output.asStateFlow()
    private val messages = Mutex()
    private var dismissMessage: CompletableDeferred<Unit>? = null
    private val visible = MutableStateFlow(false)

    init {
        core.scope.launch { languages.collectLatest { resources.value = ShopDetailIosResources(it) } }
        observeState()
        observeEvents()
    }
    fun visibility(shown: Boolean) {
        visible.value = shown && core.isCurrent()
        if (visible.value) core.resume()
    }
    fun formatChanged() { revision.value++ }
    fun close() {
        dismissMessage?.cancel()
        core.close()
        output.value = null
    }

    private fun observeState() {
        core.scope.launch {
            combine(resources.filterNotNull(), core.state, revision, message) { resources, state, _, message -> Triple(resources, state, message) }
                .collectLatest { (resources, state, message) ->
                    if (!core.isCurrent()) return@collectLatest
                    val shop = state.details?.overview
                    val distance = shop?.latitude?.let { lat -> shop.longitude?.let { lon -> distance(lat, lon) } }
                    val data = resources.shopDetailIosState(state, distance, formatDate, formatMenu, message, resources.language)
                    if (core.isCurrent()) output.value = ShopDetailIosPresentation(data, ::perform,
                        { dismissMessage?.complete(Unit) }, { analytics.action(ShopActionMetric.Edit) }, resources.language)
                    core.openLinkedCheckIn(linkedCheckIn)
                }
        }
    }

    private fun observeEvents() {
        core.scope.launch { visible.collectLatest { shown ->
            if (!shown) return@collectLatest
            core.events.collect { envelope ->
                val resources = resources.filterNotNull().first()
                if (!core.isCurrent()) return@collect
                when (envelope.event) {
                    ShopDetailEvent.FavoriteFailed -> launch { showMessage(resources.text(Res.string.shop_detail_favorite_error)) }
                    ShopDetailEvent.VoteFailed -> launch { showMessage(resources.text(Res.string.shop_checkin_list_vote_error)) }
                    else -> envelope.event.toPublicEffect(resources.text(Res.string.shop_checkins_author_unknown))?.let(effect)
                }
            }
        } }
    }

    private fun perform(name: String, id: String, index: Int) {
        if (!core.isCurrent()) return
        val intent = shopDetailIosIntent(name, id, index, core.state.value) ?: return
        if (name == "hero-photo") analytics.content(ShopContentMetric.Photos, ShopContentAction.Open)
        if (name == "menu-photo") analytics.content(ShopContentMetric.Menu, ShopContentAction.Open)
        core.action(intent)
    }

    suspend fun showMessage(text: String) = messages.withLock {
        if (!core.isCurrent()) return@withLock
        val completion = CompletableDeferred<Unit>(currentCoroutineContext()[Job])
        dismissMessage = completion
        message.value = text
        try {
            if (UIAccessibilityIsVoiceOverRunning()) completion.await() else withTimeoutOrNull(4_000) { completion.await() }
        } finally {
            if (dismissMessage === completion) {
                dismissMessage = null
                message.value = null
            }
            completion.cancel()
        }
    }
}
