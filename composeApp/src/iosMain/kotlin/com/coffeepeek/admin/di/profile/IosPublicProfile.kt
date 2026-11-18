@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.profile

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.impl.IosPublicProfileSession
import com.coffeepeek.feature.profile.impl.PublicProfileSnapshot
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

/** The app owns navigation, author confirmation and account-scoped media. */
class IosPublicProfile internal constructor(
    private val profile: IosPublicProfileSession,
    private val authorActions: ProfileAuthorActions,
    private val language: LanguagePort,
    private val media: IosCatalogueMedia,
    private val slug: String,
    private val nickname: String?,
    private val avatarUrl: String?,
    val checkInsOnly: Boolean,
    private val collections: (String, String?, String?) -> UIViewController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun languageTag() = language.current().languageTag
    fun current() = profile.current().iosState(languageTag())

    fun observe(onState: (IosPublicProfileState) -> Unit, onLanguage: (String) -> Unit): IosOwnProfileObservation =
        IosOwnProfileObservation(
            profile.observe { onState(it.iosState(languageTag())) },
            profile.observeLogin { navigate(Navigator.Screen.Auth) },
            scope.launch { language.observe().collect { onLanguage(it.languageTag); onState(current()) } },
        )

    fun back() = Navigator.popBack()
    fun refresh() = profile.refresh()
    fun toggleFollow() = profile.toggleFollow()
    fun retryActivity() {
        profile.retryActivity()
        profile.retryCheckIns()
    }
    fun retryCheckIns() = profile.retryCheckIns()
    fun moreCheckIns() = profile.loadMoreCheckIns()
    fun openConnections(followers: Boolean) = profile.openConnections(followers)
    fun closeConnections() = profile.closeConnections()
    fun retryConnections() = profile.retryConnections()
    fun moreConnections() = profile.loadMoreConnections()

    fun openUser(slug: String) {
        if (slug.isBlank()) return
        val person = profile.current().connections.firstOrNull { it.slug == slug } ?: return
        profile.closeConnections()
        navigate(Navigator.Screen.UserProfile(person.slug, person.userName, person.avatarUrl))
    }

    fun openCheckIns() {
        if (visible() != null) navigate(Navigator.Screen.UserCheckIns(slug, nickname, avatarUrl))
    }

    fun openShop(slug: String) {
        if (visible() != null && slug.isNotBlank()) navigate(Navigator.Screen.ShopDetail(slug))
    }

    fun openCoffee(slug: String) {
        if (visible() != null && slug.isNotBlank()) navigate(Navigator.Screen.CoffeeDetail(slug))
    }

    fun share() { profile.current().shareUrl()?.let(ShareHelper::shareText) }

    fun blockOrUnblock() {
        val state = profile.current()
        val target = state.profile ?: return
        if (state.own) return
        val author = ProfileAuthor(target.authorId, target.slug, target.nickname, target.avatarUrl)
        if (target.isBlockedByViewer) authorActions.requestUnblock(author) else authorActions.requestBlock(author)
    }

    fun reportCheckIn(id: String) {
        val state = visible() ?: return
        if (state.signedIn && !state.own && state.checkIns.any { it.checkIn.id == id })
            navigate(Navigator.Screen.ReportCheckIn(id))
    }

    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosOwnProfileObservation = IosOwnProfileObservation(scope.launch {
        val state = visible() ?: return@launch
        val image = media.loadImage(url)
        val current = visible()
        if (current != null && current.visibilityRevision == state.visibilityRevision && current.profile?.slug == state.profile?.slug)
            onImage(image)
    })

    fun makeAvatarViewer(onDismiss: () -> Unit): UIViewController? {
        val avatar = visible()?.profile?.avatarUrl?.takeIf(String::isNotBlank) ?: return null
        return viewer(CoffeePeekPhotoViewerImages(listOf(avatar), listOf(avatar)), 0, onDismiss)
    }

    fun makeCheckInViewer(id: String, index: Int, onDismiss: () -> Unit): UIViewController? {
        val row = visible()?.checkIns?.firstOrNull { it.checkIn.id == id } ?: return null
        if (!row.canOpenPhoto(index)) return null
        return viewer(CoffeePeekPhotoViewerImages(row.previewPhotos, row.originalPhotos), index, onDismiss)
    }

    fun makeFeaturedCollection(): UIViewController? {
        val state = visible() ?: return null
        val target = state.profile?.slug ?: return null
        return collections(if (state.own) "Owner" else "Featured", null, target)
    }

    fun makeShelfPreview(): UIViewController? {
        val state = visible() ?: return null
        val target = state.profile?.slug ?: return null
        return ComposeUIViewController { AppContent { ProfileShelfPreview(target, state.own) } }
    }

    fun close() {
        scope.cancel()
        profile.close()
    }

    private fun visible(): PublicProfileSnapshot? = profile.current().takeIf { it.profile?.isBlockedByViewer == false }

    private fun navigate(destination: Navigator.Screen) {
        if (profile.isCurrent()) Navigator.navigate(destination)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun viewer(images: CoffeePeekPhotoViewerImages, index: Int, onDismiss: () -> Unit) =
        ComposeUIViewController(configure = { opaque = false }) { AppContent { CoffeePeekPhotoViewer(images, index, onDismiss) } }
}
