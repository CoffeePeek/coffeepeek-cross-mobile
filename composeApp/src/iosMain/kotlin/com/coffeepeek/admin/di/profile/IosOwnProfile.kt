@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.media.IosPhotoViewer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.feature.checkinhistory.impl.IosOwnProfileHistorySession
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.impl.IosOwnProfileSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

/** Root tab actions use the same route entry points as the Compose Profile tab. */
class IosOwnProfile internal constructor(
    private val profile: IosOwnProfileSession,
    private val lease: CheckInHistorySession,
    private val history: IosOwnProfileHistorySession,
    private val language: LanguagePort,
    private val media: IosCatalogueMedia,
    private val shelfController: (() -> Boolean) -> UIViewController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun current() = profile.current().iosHistoryState(history.current(), languageTag())
    fun languageTag() = language.current().languageTag
    fun shelfRequested() = Navigator.pendingProfileShelf.value && isCurrent()
    fun checkInsRequested() = Navigator.pendingProfileCheckIns.value && isCurrent()
    fun observe(onState: (IosOwnProfileState) -> Unit, onLanguage: (String) -> Unit,
        onShelf: () -> Unit, onCheckIns: () -> Unit): IosOwnProfileObservation {
        return IosOwnProfileObservation(
            profile.observe { onState(current()) },
            history.observe { onState(current()) },
            scope.launch { language.observe().collect { onLanguage(it.languageTag); onState(current()) } },
            scope.launch { Navigator.pendingProfileShelf.collect { if (it && isCurrent()) onShelf() } },
            scope.launch { Navigator.pendingProfileCheckIns.collect { if (it && isCurrent()) onCheckIns() } },
        )
    }

    fun activity(expanded: Boolean) = history.activity(expanded).iosActivity(expanded)
    fun observeActivity(expanded: Boolean, receive: (IosOwnProfileActivity) -> Unit): IosOwnProfileObservation {
        return IosOwnProfileObservation(history.observe { if (isCurrent()) receive(activity(expanded)) })
    }

    fun selectDay(date: String) { if (isCurrent()) history.selectDay(date) }
    fun clearSelectedDay() { if (isCurrent()) history.selectDay(null) }
    fun changeMonth(offset: Int) { if (isCurrent()) history.changeMonth(offset) }
    fun retryActivity(expanded: Boolean) { if (isCurrent()) history.retryCalendar() }
    fun moreCheckIns() { if (isCurrent()) history.more() }
    fun resume() { if (isCurrent()) history.resume() }

    fun refresh() {
        profile.refresh()
        if (isCurrent()) history.refresh()
    }
    fun retryCheckIns() { if (isCurrent()) history.retry() }
    fun openFollowers() = profile.openFollowers()
    fun closeFollowers() = profile.closeFollowers()
    fun retryFollowers() = profile.retryFollowers()
    fun moreFollowers() = profile.moreFollowers()
    fun logout() = profile.logout()

    fun edit() = navigate(Navigator.Screen.EditProfile)
    fun openShop(slug: String) { if (slug.isNotBlank()) navigate(Navigator.Screen.ShopDetail(slug)) }
    fun openCoffee(slug: String) { if (slug.isNotBlank()) navigate(Navigator.Screen.CoffeeDetail(slug)) }
    fun openCheckIn(key: String) {
        val row = history.current().rows.firstOrNull { it.key == key } ?: return
        row.home?.let { navigate(Navigator.Screen.HomeCheckInDetail(it.id)) }
        row.shop?.let { navigate(Navigator.Screen.CheckInDetail(it.id)) }
    }
    fun openBlocked() = navigate(Navigator.Screen.BlockedAuthors)
    fun openCollections() { if (isCurrent()) openProfileCollections() }
    fun consumeShelfRequest() { if (isCurrent()) Navigator.consumeProfileShelf() }
    fun consumeCheckInsRequest() { if (isCurrent()) Navigator.consumeProfileCheckIns() }

    fun openFollower(slug: String) {
        val person = profile.current().followers.firstOrNull { it.slug == slug } ?: return
        profile.closeFollowers()
        navigate(Navigator.Screen.UserProfile(person.slug, person.userName, person.avatarUrl))
    }

    fun openContribution(kind: String) {
        if (!isCurrent()) return
        val contribution = when (kind) {
            "changes" -> ProfileContribution.Changes
            "shops" -> ProfileContribution.Shops
            "roasters" -> ProfileContribution.Roasters
            else -> return
        }
        openProfileContributions(contribution)
    }

    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosOwnProfileObservation {
        return IosOwnProfileObservation(scope.launch {
            try {
                if (!isCurrent()) {
                    onImage(null)
                    return@launch
                }
                lease.withCurrentAccount {
                    media.observeImage(url, ::isCurrent).collect { onImage(it.takeIf { isCurrent() }) }
                    Result.success(Unit)
                }
            } catch (cancelled: CancellationException) {
                onImage(null)
                throw cancelled
            }
        })
    }

    fun avatarViewer(): IosPhotoViewer? {
        val state = profile.current()
        val avatar = state.publicProfile?.avatarUrl ?: state.identity?.avatarUrl ?: return null
        if (!isCurrent() || avatar.isBlank()) return null
        return media.viewer(CoffeePeekPhotoViewerImages(listOf(avatar), listOf(avatar)), 0, ::isCurrent)
    }

    fun checkInViewer(id: String, index: Int): IosPhotoViewer? {
        val row = history.current().rows.firstOrNull { it.key == id } ?: return null
        if (!isCurrent() || !row.canOpenPhoto(index)) return null
        return media.viewer(CoffeePeekPhotoViewerImages(row.previews, row.originals), index, ::isCurrent)
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun makeCollections(): UIViewController = ComposeUIViewController(configure = { opaque = false }) {
        AppContent { ProfileOwnerCollection() }
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun makeCollectionPreferences(): UIViewController = ComposeUIViewController(configure = { opaque = false }) {
        AppContent { Column(Modifier.verticalScroll(rememberScrollState())) { ProfileDisplayPreferences() } }
    }

    fun makeShelf(): UIViewController = shelfController(::isCurrent)

    fun close() {
        lease.revoke()
        scope.cancel()
        profile.close()
        history.close()
    }

    private fun isCurrent() = profile.isCurrent() && lease.isCurrent() && lease.isLoggedIn

    private fun navigate(screen: Navigator.Screen) {
        if (isCurrent()) Navigator.navigate(screen)
    }
}

class IosOwnProfileObservation internal constructor(private vararg val jobs: Job) {
    fun cancel() = jobs.forEach(Job::cancel)
}
