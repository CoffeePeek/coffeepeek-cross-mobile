@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.feature.checkinhistory.impl.IosProfileActivitySession
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.impl.IosOwnProfileSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
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
    private val calendar: (Boolean) -> IosProfileActivitySession,
    private val language: LanguagePort,
    private val media: IosCatalogueMedia,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val week = lazy { calendar(false) }
    private val month = lazy { calendar(true) }

    fun current() = profile.current().iosState(languageTag())
    fun languageTag() = language.current().languageTag
    fun shelfRequested() = Navigator.pendingProfileShelf.value && isCurrent()
    fun observe(onState: (IosOwnProfileState) -> Unit, onLanguage: (String) -> Unit, onShelf: () -> Unit): IosOwnProfileObservation {
        return IosOwnProfileObservation(
            profile.observe { onState(it.iosState(languageTag())) },
            scope.launch { language.observe().collect { onLanguage(it.languageTag); onState(current()) } },
            scope.launch { Navigator.pendingProfileShelf.collect { if (it && isCurrent()) onShelf() } },
        )
    }

    fun activity(expanded: Boolean) = activityModel(expanded).current().iosActivity(expanded)
    fun observeActivity(expanded: Boolean, receive: (IosOwnProfileActivity) -> Unit): IosOwnProfileObservation {
        val model = activityModel(expanded)
        val jobs = model.observe({ if (isCurrent()) receive(it.iosActivity(expanded)) }, ::openShop, ::openHistory)
        model.visible()
        return IosOwnProfileObservation(*jobs.toTypedArray())
    }

    fun selectDay(date: String) { if (isCurrent()) month.value.selectDay(date) }
    fun changeMonth(offset: Int) { if (isCurrent()) month.value.changeMonth(offset) }
    fun retryActivity(expanded: Boolean) { if (isCurrent()) activityModel(expanded).retry() }
    fun openWeek() { if (isCurrent()) week.value.openWeek() }
    fun openHistoryRow(key: String) { if (isCurrent()) month.value.openRow(key) }

    fun refresh() = profile.refresh()
    fun retryCheckIns() = profile.retryCheckIns()
    fun openFollowers() = profile.openFollowers()
    fun closeFollowers() = profile.closeFollowers()
    fun retryFollowers() = profile.retryFollowers()
    fun moreFollowers() = profile.moreFollowers()
    fun logout() = profile.logout()

    fun edit() = navigate(Navigator.Screen.EditProfile)
    fun openHistory() = navigate(Navigator.Screen.VisitedPlaces)
    fun openShop(slug: String) { if (slug.isNotBlank()) navigate(Navigator.Screen.ShopDetail(slug)) }
    fun openCoffee(slug: String) { if (slug.isNotBlank()) navigate(Navigator.Screen.CoffeeDetail(slug)) }
    fun openBlocked() = navigate(Navigator.Screen.BlockedAuthors)
    fun openCollections() { if (isCurrent()) openProfileCollections() }
    fun consumeShelfRequest() { if (isCurrent()) Navigator.consumeProfileShelf() }

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
            if (!isCurrent()) return@launch
            val image = lease.withCurrentAccount { Result.success(media.loadImage(url)) }.getOrNull()
            if (isCurrent()) onImage(image)
        })
    }

    fun makeAvatarViewer(onDismiss: () -> Unit): UIViewController? {
        val state = profile.current()
        val avatar = state.publicProfile?.avatarUrl ?: state.identity?.avatarUrl ?: return null
        if (!isCurrent() || avatar.isBlank()) return null
        return viewer(CoffeePeekPhotoViewerImages(listOf(avatar), listOf(avatar)), 0, onDismiss)
    }

    fun makeCheckInViewer(id: String, index: Int, onDismiss: () -> Unit): UIViewController? {
        val row = profile.current().checkIns.firstOrNull { it.checkIn.id == id } ?: return null
        if (!isCurrent() || !row.canOpenPhoto(index)) return null
        return viewer(CoffeePeekPhotoViewerImages(row.previewPhotos, row.originalPhotos), index, onDismiss)
    }

    fun makeCollections(): UIViewController = ComposeUIViewController { AppContent { ProfileOwnerCollection() } }

    fun makeCollectionPreferences(): UIViewController = ComposeUIViewController {
        AppContent { Column(Modifier.verticalScroll(rememberScrollState())) { ProfileDisplayPreferences() } }
    }

    fun makeShelf(): UIViewController = ComposeUIViewController {
        AppContent { ProvideFloatingNavClearance(72.dp) { ProfileOwnShelf(Modifier) {} } }
    }

    fun close() {
        lease.revoke()
        scope.cancel()
        profile.close()
        if (week.isInitialized()) week.value.close()
        if (month.isInitialized()) month.value.close()
    }

    private fun isCurrent() = profile.isCurrent() && lease.isCurrent() && lease.isLoggedIn
    private fun activityModel(expanded: Boolean) = if (expanded) month.value else week.value

    private fun navigate(screen: Navigator.Screen) {
        if (isCurrent()) Navigator.navigate(screen)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun viewer(images: CoffeePeekPhotoViewerImages, index: Int, onDismiss: () -> Unit) =
        ComposeUIViewController(configure = { opaque = false }) { AppContent { CoffeePeekPhotoViewer(images, index, onDismiss) } }
}

class IosOwnProfileObservation internal constructor(private vararg val jobs: Job) {
    fun cancel() = jobs.forEach(Job::cancel)
}
