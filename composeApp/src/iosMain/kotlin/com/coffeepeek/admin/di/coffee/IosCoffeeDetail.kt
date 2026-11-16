@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.nativeScreenHost
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.coffee.impl.IosCoffeeDetailSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

class IosCoffeeDetailObservation internal constructor(private vararg val jobs: Job) {
    fun cancel() = jobs.forEach { it.cancel() }
}

/** Keeps network work and navigation behind the same Kotlin actions as Compose. */
class IosCoffeeDetail internal constructor(
    private val session: IosCoffeeDetailSession,
    private val media: IosCatalogueMedia,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val analytics = IosCoffeeDetailAnalytics(scope)

    fun currentState(): IosCoffeeDetailState = session.currentState().toIosState()

    fun observe(onState: (IosCoffeeDetailState) -> Unit): IosCoffeeDetailObservation {
        val state = session.observeState { snapshot ->
            analytics.updateLoaded(snapshot.details != null)
            onState(snapshot.toIosState())
        }
        val events = session.observeEvents(
            onBack = Navigator::popBack,
            onCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
            onRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onSeller = OpenInBrowser::openInBrowser,
            onShare = { name, path -> ShareHelper.shareText("$name — https://coffeepeek.by$path") },
        )
        return IosCoffeeDetailObservation(state, events)
    }

    fun retry() = session.retry()
    fun retrySimilar() = session.retrySimilar()
    fun back() = session.back()
    fun openPhotos() = session.openPhotos()
    fun toggleDescription() = session.toggleDescription()
    fun openRoaster() = session.openRoaster()
    fun openSimilar(slug: String) = session.openSimilar(slug)
    fun openSeller(index: Int) = session.openSeller(index)
    fun share() = session.share()

    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosCoffeeDetailObservation =
        IosCoffeeDetailObservation(scope.launch { onImage(media.loadImage(url)) })

    fun makeViewer(): UIViewController? = session.currentState().details?.photos?.takeIf { it.isNotEmpty() }
        ?.let { urls -> media.makeViewer(urls, 0, session::closePhotos) }

    fun visible(host: UIViewController) = analytics.updateOwner(host.nativeScreenHost()?.analyticsVisit)
    fun hidden() = analytics.updateOwner(null)

    fun updateExposure(section: String, fraction: Float) = analytics.updateFraction(section, fraction)

    fun close() {
        scope.cancel()
        session.close()
    }
}
