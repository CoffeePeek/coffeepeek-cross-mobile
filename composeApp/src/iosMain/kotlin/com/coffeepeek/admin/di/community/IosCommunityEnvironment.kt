@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.admin.di.community

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.admin.di.catalog.methodLabel
import com.coffeepeek.admin.di.media.photoPresentation
import com.coffeepeek.admin.ui.component.ReviewDateLabels
import com.coffeepeek.admin.ui.component.formatReviewDisplayDate
import com.coffeepeek.admin.ui.component.nativeReviewDateLabels
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityNativeHost
import com.coffeepeek.feature.community.impl.NativeCommunityPhoto
import com.coffeepeek.feature.community.impl.NativeCommunitySheet
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.*

/** Root assembly of existing media, locale and account services for the native feature owner. */
internal class IosCommunityEnvironment(private val scope: CoroutineScope, private val accounts: SessionReader,
    private val language: LanguagePort, fetcher: ImageUrlFetcher,
) {
    private val generation = accounts.accountGeneration()
    private val user = accounts.peekSession()?.userId
    private val signedIn = accounts.isActiveSession(accounts.peekSession())
    private val overlayId = correlationId("overlay")
    private var dateLabels: ReviewDateLabels? = null
    private var closed = false
    val overlays = IosCommunityOverlays(scope, ::isCurrent, { json, busy, actions -> IosCommunitySheet(json, busy, actions, accounts) }) { photo ->
        photoPresentation(CoffeePeekPhotoViewerImages(photo.thumbnails, photo.originals), photo.index, accounts, fetcher, language, photo.dismiss)
    }
    private val languages = language.observe().mapLatest { selection ->
        dateLabels = nativeReviewDateLabels()
        selection.languageTag
    }.shareIn(scope, SharingStarted.Eagerly, replay = 1)
    val host = CommunityNativeHost(languages, { formatReviewDisplayDate(it, requireNotNull(dateLabels)) },
        { it.methodLabel(it) }, ShareHelper::shareText, overlays::message)

    fun observeAccount(revoke: () -> Unit) {
        scope.launch { accounts.observeSession().catch {
            if (it is CancellationException) throw it
            revoke()
        }.collect { if (!isCurrent()) revoke() } }
    }

    fun isCurrent() = !closed && accounts.accountGeneration() == generation && accounts.peekSession()?.userId == user &&
        accounts.isActiveSession(accounts.peekSession()) == signedIn

    fun update(sheet: NativeCommunitySheet?, photo: NativeCommunityPhoto?, clearance: Double) {
        if (!isCurrent()) return
        overlays.update(sheet, photo, clearance)
        Analytics.service?.overlay(overlayId, sheet != null)
    }

    fun close() {
        if (closed) return
        closed = true
        overlays.close()
        Analytics.service?.overlay(overlayId, false)
    }

}
