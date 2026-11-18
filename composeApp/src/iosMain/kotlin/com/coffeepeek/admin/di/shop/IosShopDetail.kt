package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosPresentation
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosState
import com.coffeepeek.feature.shop.impl.ui.withoutContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

/** Bound to the same browsing lease as the shared detail ViewModel, votes and favorite writes. */
class IosShopDetail internal constructor(
    initial: ShopDetailIosPresentation, private val media: IosCatalogueMedia,
    private val lease: ShopBrowsingSession,
    private val ownerFactory: (String, String, () -> Boolean, () -> Unit, () -> Unit) -> UIViewController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var presentation: ShopDetailIosPresentation? = initial
    private var latestState = initial.state
    private var closed = false
    private val state = MutableStateFlow(IosShopDetailState(initial.state, true))
    internal val exposures = MutableStateFlow<Set<String>>(emptySet())
    private val initialLanguageTag = initial.languageTag
    val languageTag get() = presentation?.languageTag ?: initialLanguageTag

    fun currentState() = state.value
    fun observe(onState: (IosShopDetailState) -> Unit) = IosShopCheckInObservation(scope.launch { state.collect(onState) })

    internal fun update(next: ShopDetailIosPresentation) {
        if (!allowed()) return
        presentation = next
        latestState = next.state
        state.value = IosShopDetailState(next.state, true)
    }

    fun perform(name: String, id: String, index: Int) {
        if (allowed()) presentation?.onAction?.invoke(name, id, index)
    }

    fun dismissMessage() { if (allowed()) presentation?.onDismissMessage?.invoke() }
    fun visible(names: List<String>) { exposures.value = names.toSet().takeIf { allowed() }.orEmpty() }

    fun loadImage(url: String, onImage: (UIImage?) -> Unit) = IosShopCheckInObservation(scope.launch {
        if (url.isBlank() || !allowed()) { onImage(null); return@launch }
        try { media.observeImage(url, ::allowed).collect { onImage(it.takeIf { allowed() }) } }
        catch (cancelled: CancellationException) {
            onImage(null)
            throw cancelled
        }
        catch (_: Exception) { onImage(null) }
    })

    fun loadAvatar(id: String, receive: (UIImage?) -> Unit): IosShopCheckInObservation =
        loadImage(presentation?.state?.checkIns?.rows?.firstOrNull { it.id == id }?.avatarUrl.orEmpty(), receive)

    fun loadPhoto(id: String, index: Int, receive: (UIImage?) -> Unit): IosShopCheckInObservation =
        loadImage(presentation?.state?.checkIns?.rows?.firstOrNull { it.id == id }?.thumbnail(index).orEmpty(), receive)

    fun ownershipController(): UIViewController? {
        val shop = presentation?.state?.details ?: return null
        return ownerFactory(shop.id, shop.slug, ::allowed,
            { perform("suggest", "", 0) }, { if (allowed()) presentation?.onOwnerEdit?.invoke() })
    }

    internal fun close() {
        latestState = latestState.withoutContent(languageTag)
        closed = true
        presentation = null
        exposures.value = emptySet()
        state.value = IosShopDetailState(latestState, false)
        scope.cancel()
    }

    private fun allowed() = !closed && lease.isCurrent()
}

class IosShopDetailState internal constructor(content: ShopDetailIosState, val active: Boolean) {
    val data = content.toNative()
    val checkIns = IosShopCheckInListState(content.generation, content.checkIns)
}
