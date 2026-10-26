package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.component.rememberReviewDisplayDate
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PrefetchListPhotos
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.api.PublicProfileEntry
import com.coffeepeek.feature.profile.impl.createPublicProfileEntry

/** Both platforms use the feature public profile; navigation and platform media stay app-owned. */
internal class FeaturePublicProfileScreenRenderer(private val entry: PublicProfileEntry) : PublicProfileScreenRenderer {
    @Composable
    override fun Content(slug: String, nickname: String?, avatarUrl: String?, checkInsOnly: Boolean) {
        entry.Content(slug, nickname, avatarUrl,
            onBack = Navigator::popBack,
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenUser = { userSlug, userNickname, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(userSlug, userNickname, avatar)) },
            onShare = { url -> ShareHelper.shareText(url) },
            onOpenCheckIns = { Navigator.navigate(Navigator.Screen.UserCheckIns(slug, nickname, avatarUrl)) },
            checkInsOnly = checkInsOnly, modifier = Modifier)
    }
}

internal class AppProfileAuthorActions(private val blocks: AuthorBlockActions) : ProfileAuthorActions {
    override fun requestBlock(author: ProfileAuthor) = blocks.requestBlock(author.toReference())
    override fun requestUnblock(author: ProfileAuthor) = blocks.requestUnblock(author.toReference())
}

private fun ProfileAuthor.toReference() = AuthorReference(id, slug, name, avatarUrl)

internal fun featurePublicProfileScreenRenderer(
    repository: PublicProfileData,
    checkIns: PublicCheckInsPort,
    accounts: ProfileAccountPort,
    visibility: AuthorVisibility,
    blocks: AuthorBlockActions,
): FeaturePublicProfileScreenRenderer {
    val slots = profileSlots()
    return FeaturePublicProfileScreenRenderer(createPublicProfileEntry(
        data = repository,
        checkIns = checkIns,
        accounts = accounts,
        visibility = AppProfileAuthorVisibility(visibility),
        authorActions = AppProfileAuthorActions(blocks),
        slots = slots,
    ))
}


/** Platform media, formatting and routing; feature-owned card content is never injected here. */
internal fun profileSlots(): ProfileSlots {
    return ProfileSlots(
        image = { url, description, modifier -> CpImage(data = url, modifier = modifier, contentScale = ContentScale.Crop, contentDescription = description) },
        photoViewer = { previews, originals, index, onDismiss -> FullScreenImageDialog(PhotoViewerImages(previews, originals), index, onDismiss) },
        logoImage = { url, description, modifier -> CpImage(data = url, modifier = modifier, contentScale = ContentScale.Fit, contentDescription = description) },
        displayDate = { rememberReviewDisplayDate(it) },
        progressiveImage = { preview, detail, modifier -> CpImage(data = preview, highQualityData = detail,
            modifier = modifier, contentScale = ContentScale.Crop) },
        reportCheckIn = { Navigator.navigate(Navigator.Screen.ReportCheckIn(it)) },
        photoPrefetch = { listState, items -> PrefetchListPhotos(listState, items) },
    )
}
