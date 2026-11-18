@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shelf

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

/** One owner generation. Swift subscriptions can pause without dropping the shared feature session. */
class IosShelfScreen internal constructor(val kind: String, val languageTag: String,
    feature: IosShelfFeature, profiles: ShelfProfileRepository, slug: String?, own: Boolean,
    private val account: SessionShelfAccount, private val media: IosCatalogueMedia,
    private val checkIns: FeatureCheckInCreateFlowRenderer, private val allowed: () -> Boolean,
) {
    private val owner = account.visibilityScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var closed = false
    private var revoked = false
    private val shelf = if (kind == "shelf" && owner != null) feature.shelf() else null
    private val bag = if (kind == "bag" && owner != null && slug != null) feature.bag(slug) else null
    private val add = if (kind == "add" && owner != null) feature.add() else null
    private val preview = if (kind == "public" && slug != null) createIosShelfProfileSession(profiles, slug, own) else null
    private val visibility = if (kind == "shelf" && owner != null) createIosShelfVisibilitySession(profiles) else null
    private var camera: IosShelfNativeCamera? = null
    private val picker = ShelfPickerDelegate({ photo -> if (isCurrent()) add?.photo(photo.bytes, photo.fromCamera) },
        { if (isCurrent()) add?.action("photoFailed") })

    init { scope.launch { account.visibilityScopes.first { it != owner }; revoke() } }

    fun current(): IosShelfState {
        if (!isCurrent()) return IosShelfState()
        return shelf?.current()?.iosState(visibility?.current()) ?: bag?.current()?.iosState()
            ?: add?.current()?.iosState() ?: preview?.current()?.iosState() ?: IosShelfState()
    }

    fun observe(receive: (IosShelfState) -> Unit): IosShelfObservation {
        val jobs = listOfNotNull(shelf?.observe { receive(current()) }, bag?.observe { receive(current()) },
            add?.observe { receive(current()) }, preview?.observe { receive(current()) }, visibility?.observe { receive(current()) },
            shelf?.events(::navigate), bag?.events(::navigate), preview?.events { navigate("coffee", it) },
            add?.events { saved -> if (isCurrent()) { if (saved) Navigator.returnToProfileShelf() else Navigator.popBack() } },
            scope.launch { account.visibilityScopes.first { it != owner }; receive(IosShelfState()) })
        return IosShelfObservation(jobs)
    }

    fun action(value: String, id: String?) {
        if (value == "back") { stopCamera(); Navigator.popBack(); return }
        if (!isCurrent()) return
        when (kind) {
            "shelf" -> shelfAction(value, id)
            "public" -> if (value == "open" && id != null) preview?.open(id) else preview?.refresh()
            "bag" -> bag?.action(value)
            "add" -> { if (value == "closeCamera" || value == "close") stopCamera(); add?.action(value) }
        }
    }

    private fun shelfAction(value: String, id: String?) {
        when (value) {
            "add" -> shelf?.add()
            "archive" -> shelf?.archive()
            "open" -> id?.let { shelf?.open(it) }
            "finish" -> id?.let { shelf?.startFinish(it) }
            "cancelFinish" -> shelf?.cancelFinish()
            "undo" -> shelf?.undo()
            "dismiss" -> shelf?.dismissMessage()
            "arrived" -> id?.let { shelf?.arrived(it) }
            "retryVisibility" -> visibility?.refresh()
            else -> shelf?.refresh()
        }
    }

    fun finish(id: String, rating: Int?, buyAgain: Boolean?) {
        if (!isCurrent()) return
        if (kind == "bag") bag?.finish(rating, buyAgain) else shelf?.finish(id, rating, buyAgain)
    }
    fun move(id: String, position: Int) { if (isCurrent()) shelf?.move(id, position) }
    fun setVisible(value: Boolean) { if (isCurrent()) visibility?.set(value) }
    fun query(value: String) { if (isCurrent()) add?.query(value) }
    fun coffee(slug: String) { if (isCurrent()) add?.coffee(slug) }
    fun roaster(value: String) { if (isCurrent()) add?.roaster(value) }
    fun weight(grams: Int?) { if (isCurrent()) add?.weight(grams) }
    fun grind(value: String?) { if (isCurrent()) add?.grind(value) }
    fun purpose(value: String?) { if (isCurrent()) add?.purpose(value) }
    fun date(value: String?) { if (isCurrent()) add?.date(value) }
    fun save(later: Boolean) { if (isCurrent()) add?.save(later) }
    fun gallery() { if (isCurrent()) presentShelfPicker(picker) }
    fun photo(card: Boolean): UIImage? = if (isCurrent()) (if (card) add?.cardBytes() else add?.photoBytes())?.toShelfImage() else null

    fun loadCover(id: String, receive: (UIImage?) -> Unit): IosShelfObservation = IosShelfObservation(listOf(scope.launch {
        if (!isCurrent()) return@launch
        if (kind == "public") loadPublicCover(id, receive) else {
            val url = if (kind == "bag") bag?.photo() else shelf?.photo(id)
            if (url != null) media.observeImage(url, ::isCurrent).collect { receive(it.takeIf { isCurrent() }) }
        }
    }))

    private suspend fun loadPublicCover(id: String, receive: (UIImage?) -> Unit) {
        val revision = preview?.current()?.revision
        val bytes = preview?.photo(id) ?: return
        try { if (isCurrent() && preview?.current()?.revision == revision) receive(bytes.toShelfImage()) }
        finally { bytes.fill(0) }
    }

    fun startCamera(): IosShelfNativeCamera? {
        if (!isCurrent()) return null
        stopCamera()
        return IosShelfNativeCamera(scope, { isCurrent() && add?.current()?.step == "Camera" },
            { bytes -> stopCamera(); add?.photo(bytes, true) }, { add?.action("photoFailed") }).also { camera = it }
    }
    fun stopCamera() { camera?.close(); camera = null }

    /** The existing native Home editor remains hosted by its composition entry (including analytics and photo handoff). */
    fun makeBrew(): UIViewController? {
        val coffee = bag?.brew()?.takeIf { isCurrent() } ?: return null
        return ComposeUIViewController {
            AppContent {
                checkIns.Home(HomeCheckInBean(coffee.slug, coffee.name, coffee.shelfBagId),
                    onDismiss = { bag?.brewed(false) }, onSaved = { bag?.brewed(true) })
            }
        }
    }

    fun closeBrew() { if (isCurrent()) bag?.brewed(false) }

    fun close() {
        if (closed) return
        closed = true
        revoke()
        scope.cancel()
    }
    private fun revoke() {
        if (revoked) return
        revoked = true
        stopCamera()
        shelf?.close()
        bag?.close()
        add?.close()
        preview?.close()
        visibility?.close()
    }
    private fun isCurrent() = !closed && !revoked && owner == account.visibilityScope && allowed()
    private fun navigate(kind: String, id: String?) {
        if (!isCurrent()) return
        when (kind) {
            "add" -> Navigator.navigate(Navigator.Screen.ShelfAddBag)
            "archive" -> Navigator.navigate(Navigator.Screen.ShelfArchive)
            "bag" -> id?.let { Navigator.navigate(Navigator.Screen.ShelfBag(it)) }
            "coffee" -> id?.let { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) }
            "signIn" -> Navigator.navigate(Navigator.Screen.Auth)
            "back" -> Navigator.popBack()
        }
    }
}

class IosShelfObservation internal constructor(private val jobs: List<Job>) {
    fun cancel() = jobs.forEach { it.cancel() }
}

private fun ByteArray.toShelfImage(): UIImage? = toShelfData()?.let { UIImage.imageWithData(it) }
