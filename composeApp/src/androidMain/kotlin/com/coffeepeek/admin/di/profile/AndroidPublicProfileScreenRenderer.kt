package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.admin.feature.favorites.api.FavoritesPreviewCarousel
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn as LegacyCheckIn
import com.coffeepeek.admin.feature.profile.domain.PublicProfileRepository
import com.coffeepeek.admin.feature.profile.ui.PublicProfileCheckInCard
import com.coffeepeek.admin.feature.profile.ui.PublicProfileFavoriteCard
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PrefetchListPhotos
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.api.PublicProfileEntry
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileCheckInPage
import com.coffeepeek.feature.profile.impl.createPublicProfileEntry

/** Android routes to the migrated public profile; navigation, sharing and the legacy-rendered cards stay app-owned. */
internal class AndroidPublicProfileScreenRenderer(private val entry: PublicProfileEntry) : PublicProfileScreenRenderer {
    @Composable
    override fun Content(slug: String, nickname: String?, avatarUrl: String?) {
        entry.Content(slug, nickname, avatarUrl,
            onBack = Navigator::popBack,
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenUser = { userSlug, userNickname, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(userSlug, userNickname, avatar)) },
            onShare = { url -> ShareHelper.shareText(url) })
    }
}

/**
 * Check-ins still render through the shared timeline card, which needs the legacy `CheckIn`. This adapter hands the feature its
 * own model and keeps the legacy rows (bounded, most recent first) for the card slot of the same ids.
 */
internal class AndroidPublicCheckIns(private val repository: PublicProfileRepository) : PublicCheckInsPort {
    private val sources = object : LinkedHashMap<String, LegacyCheckIn>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LegacyCheckIn>) = size > MAX_REMEMBERED
    }

    override suspend fun page(slug: String, cursor: String?): Result<PublicProfileCheckInPage> =
        repository.getCheckIns(slug, cursor).map { page ->
            synchronized(sources) { page.items.forEach { sources[it.id] = it } }
            PublicProfileCheckInPage(page.items.map(LegacyCheckIn::toFeatureModel), page.nextCursor)
        }

    fun legacy(checkIn: PublicProfileCheckIn): LegacyCheckIn = synchronized(sources) { sources[checkIn.id] } ?: LegacyCheckIn(
        checkIn.id, checkIn.coffeeShopSlug, checkIn.venue, checkIn.title, checkIn.details, checkIn.rating, checkIn.note,
        checkIn.occurredAt, checkIn.photoUrls,
    )

    private companion object { const val MAX_REMEMBERED = 200 }
}

internal fun LegacyCheckIn.toFeatureModel() = PublicProfileCheckIn(
    id, coffeeShopSlug, venue, title, details, rating, note, occurredAt, photoUrls,
    photoPreviewUrls = source?.photoThumbnailUrls ?: photoUrls,
)

internal class AndroidProfileAuthorActions(private val blocks: AuthorBlockActions) : ProfileAuthorActions {
    override fun requestBlock(author: ProfileAuthor) = blocks.requestBlock(author.toReference())
    override fun requestUnblock(author: ProfileAuthor) = blocks.requestUnblock(author.toReference())
}

private fun ProfileAuthor.toReference() = AuthorReference(id, slug, name, avatarUrl)

internal fun androidPublicProfileScreenRenderer(
    repository: PublicProfileRepository,
    sessions: SessionRepository,
    users: UserRepository,
    visibility: AuthorVisibility,
    blocks: AuthorBlockActions,
): AndroidPublicProfileScreenRenderer {
    val checkIns = AndroidPublicCheckIns(repository)
    val slots = androidProfileSlots(checkIns)
    return AndroidPublicProfileScreenRenderer(createPublicProfileEntry(
        data = repository,
        checkIns = checkIns,
        accounts = AndroidProfileAccountPort(sessions, users),
        visibility = AndroidProfileAuthorVisibility(visibility),
        authorActions = AndroidProfileAuthorActions(blocks),
        slots = slots,
    ))
}


/** The legacy-rendered pieces shared by the public profile and the Profile tab. */
internal fun androidProfileSlots(checkIns: AndroidPublicCheckIns): ProfileSlots {
    return ProfileSlots(
        image = { url, description, modifier -> CpImage(data = url, modifier = modifier, contentScale = ContentScale.Crop, contentDescription = description) },
        photoViewer = { previews, originals, index, onDismiss -> FullScreenImageDialog(PhotoViewerImages(previews, originals), index, onDismiss) },
        favoritesPreview = { favorites, onOpen ->
            FavoritesPreviewCarousel(
                coffeeShops = favorites.filter { it.kind == PublicFavoriteKind.CoffeeShop },
                roasters = favorites.filter { it.kind == PublicFavoriteKind.Roaster },
                drinks = favorites.filter { it.kind == PublicFavoriteKind.Drink },
                itemKey = { it.id },
            ) { favorite, modifier -> PublicProfileFavoriteCard(favorite, modifier = modifier, onClick = { onOpen(favorite) }) }
        },
        favoriteCard = { favorite, modifier, onClick -> PublicProfileFavoriteCard(favorite, modifier = modifier, onClick = onClick) },
        checkInCard = { checkIn, author, isOwnProfile, onShopClick, onPhotoClick ->
            PublicProfileCheckInCard(
                checkIn = checkIns.legacy(checkIn),
                onShopClick = { onShopClick?.invoke() },
                onPhotoClick = { images, index -> onPhotoClick(images.previews, images.originals, index) },
                authorName = author.name,
                authorAvatarUrl = author.avatarUrl,
                isOwnProfile = isOwnProfile,
                author = AuthorReference(author.id, author.slug, author.name, author.avatarUrl),
            )
        },
        photoPrefetch = { listState, items -> PrefetchListPhotos(listState, items) },
    )
}
