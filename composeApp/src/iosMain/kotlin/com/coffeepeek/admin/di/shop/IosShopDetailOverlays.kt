package com.coffeepeek.admin.di.shop

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.admin.di.media.IosCheckInPhotoPresentation
import com.coffeepeek.admin.di.media.photoPresentation
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shop.*
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopDetailContext
import com.coffeepeek.feature.shop.api.ShopDetailEffect
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString

class IosShopDetailOverlayState internal constructor(val photo: IosCheckInPhotoPresentation? = null,
    val editor: IosShopCheckInEditor? = null, val route: IosShopRouteOptions? = null)
class IosShopRouteOptions internal constructor(val title: String, val cancel: String, val options: List<IosShopRouteOption>)
class IosShopRouteOption internal constructor(val title: String, val select: () -> Unit)

/** Native UI presents these sources; shared leases remain authoritative for every action. */
class IosShopDetailOverlays internal constructor(private val scope: CoroutineScope, private val isCurrent: () -> Boolean,
    private val accounts: SessionReader, private val language: LanguagePort, private val fetcher: ImageUrlFetcher,
    private val forms: SessionShopCheckInEntry, private val submitted: () -> Unit,
) {
    private val mutable = MutableStateFlow(IosShopDetailOverlayState())
    private val overlay = correlationId("shop-detail")
    private var closed = false
    private var routeRevision = 0
    fun current() = mutable.value
    fun observe(receive: (IosShopDetailOverlayState) -> Unit) = IosShopCheckInObservation(scope.launch { mutable.collect(receive) })
    private fun active() = !closed && isCurrent()

    internal fun photo(effect: ShopDetailEffect.OpenPhoto) {
        if (!active() || mutable.value.editor != null || mutable.value.photo != null) return
        var photo: IosCheckInPhotoPresentation? = null
        photo = photoPresentation(CoffeePeekPhotoViewerImages(effect.imageUrls, effect.originalUrls), effect.initialIndex,
            accounts, fetcher, language) { photo?.let(::dismissPhoto) }
        update(IosShopDetailOverlayState(photo = photo))
    }
    fun dismissPhoto(photo: IosCheckInPhotoPresentation) {
        if (mutable.value.photo !== photo) return
        photo.close()
        update(IosShopDetailOverlayState())
    }

    internal fun editor(shop: ShopDetailContext) {
        if (!active() || mutable.value.editor != null || mutable.value.photo != null) return
        var editor: IosShopCheckInEditor? = null
        val callbacks = ShopCheckInNativeCallbacks({ editor?.let(::dismissEditor) },
            { finishEditor(editor) { Navigator.popThenNavigate(Navigator.Screen.CreateCheckIn) } },
            { if (active() && mutable.value.editor === editor) submitted() },
            { finishEditor(editor) { Navigator.popThenSelectTab(Navigator.Screen.CommunityTab) } },
            { finishEditor(editor) { Navigator.openProfileCheckIns(popCurrent = true) } })
        editor = nativeShopCheckInForm(forms, shop, language, accounts, callbacks) { editor?.let(::dismissEditor) }
        update(IosShopDetailOverlayState(editor = editor))
    }
    private fun finishEditor(editor: IosShopCheckInEditor?, then: () -> Unit) {
        if (!active() || editor == null || mutable.value.editor !== editor) return
        dismissEditor(editor)
        then()
    }
    fun dismissEditor(editor: IosShopCheckInEditor) {
        if (mutable.value.editor !== editor) return
        editor.revoke()
        update(IosShopDetailOverlayState())
    }
    fun requestEditorDismiss(editor: IosShopCheckInEditor) { if (active() && mutable.value.editor === editor) editor.dismiss() }

    internal fun route(effect: ShopDetailEffect.OpenRoute) {
        val revision = ++routeRevision
        scope.launch {
            val options = listOf(
                option(Res.string.route_apple_maps, buildAppleMapsRouteUrl(effect.latitude, effect.longitude), revision),
                option(Res.string.route_google_maps, buildGoogleMapsRouteUrl(effect.latitude, effect.longitude), revision),
                option(Res.string.route_yandex_maps, buildYandexMapsRouteUrl(effect.latitude, effect.longitude), revision))
            val data = IosShopRouteOptions(getString(Res.string.route_choose_app), getString(Res.string.check_in_create_close), options)
            if (active() && revision == routeRevision && mutable.value.photo == null && mutable.value.editor == null)
                update(IosShopDetailOverlayState(route = data))
        }
    }
    private suspend fun option(label: org.jetbrains.compose.resources.StringResource, url: String, revision: Int) =
        IosShopRouteOption(getString(label)) { if (active() && revision == routeRevision && mutable.value.route != null) {
            dismissRoute()
            OpenInBrowser.openInBrowser(url)
        } }
    fun dismissRoute() {
        routeRevision++
        if (mutable.value.route != null) update(IosShopDetailOverlayState())
    }
    private fun update(state: IosShopDetailOverlayState) {
        mutable.value = state
        Analytics.service?.overlay(overlay, state.photo != null || state.editor != null || state.route != null)
    }
    internal fun close() {
        if (closed) return
        closed = true
        mutable.value.photo?.close()
        mutable.value.editor?.revoke()
        update(IosShopDetailOverlayState())
    }
}
