package com.coffeepeek.admin.di.profile

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.admin.feature.favorites.api.FavoritesPreviewCarousel
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn as LegacyCheckIn
import com.coffeepeek.admin.feature.profile.domain.PublicProfileRepository
import com.coffeepeek.admin.feature.profile.ui.PublicProfileCheckInCard
import com.coffeepeek.admin.feature.profile.ui.PublicProfileFavoriteCard
import com.coffeepeek.admin.feature.profile.ui.PublicProfileActivityPreview
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PrefetchListPhotos
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.core.session.SessionReader
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
internal class PublicProfileFeatureScreenRenderer(private val entry: PublicProfileEntry) : PublicProfileScreenRenderer {
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

/**
 * Check-ins still render through the shared timeline card, which needs the legacy `CheckIn`. This adapter hands the feature its
 * own model and keeps the legacy rows (bounded, most recent first) for the card slot of the same ids.
 */
internal class ProfilePublicCheckIns(private val repository: PublicProfileRepository) : PublicCheckInsPort {
    private val sources = RememberedCheckIns()

    override suspend fun page(slug: String, cursor: String?): Result<PublicProfileCheckInPage> =
        repository.getCheckIns(slug, cursor).map { page ->
            sources.remember(slug, restart = cursor == null, page.items)
            PublicProfileCheckInPage(page.items.map(LegacyCheckIn::toFeatureModel), page.nextCursor)
        }

    fun legacy(checkIn: PublicProfileCheckIn): LegacyCheckIn = sources.find(checkIn.id) ?: LegacyCheckIn(
        checkIn.id, checkIn.coffeeShopSlug, checkIn.venue, checkIn.title, checkIn.details, checkIn.rating, checkIn.note,
        checkIn.occurredAt, checkIn.photoUrls,
    )
}

/**
 * Keeps the legacy rows of the profiles that were opened last, whole: a card must not change meaning because other rows pushed it out.
 * Memory is bounded by the number of remembered profiles, and a refresh of a profile starts its rows afresh.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class RememberedCheckIns(private val maxProfiles: Int = 4) {
    // Most recently opened profile last; each change swaps in a whole new snapshot, so a reader never sees half of one.
    private val byProfile = AtomicReference<List<Pair<String, Map<String, LegacyCheckIn>>>>(emptyList())

    fun remember(slug: String, restart: Boolean, items: List<LegacyCheckIn>) {
        while (true) {
            val current = byProfile.load()
            val kept = if (restart) emptyMap() else current.firstOrNull { it.first == slug }?.second.orEmpty()
            val next = (current.filterNot { it.first == slug } + (slug to kept + items.associateBy { it.id })).takeLast(maxProfiles)
            if (byProfile.compareAndSet(current, next)) return
        }
    }

    fun find(id: String): LegacyCheckIn? = byProfile.load().firstNotNullOfOrNull { it.second[id] }
}

internal fun LegacyCheckIn.toFeatureModel() = PublicProfileCheckIn(
    id, coffeeShopSlug, venue, title, details, rating, note, occurredAt, photoUrls,
    photoPreviewUrls = source?.photoThumbnailUrls ?: photoUrls,
)

internal class AppProfileAuthorActions(private val blocks: AuthorBlockActions) : ProfileAuthorActions {
    override fun requestBlock(author: ProfileAuthor) = blocks.requestBlock(author.toReference())
    override fun requestUnblock(author: ProfileAuthor) = blocks.requestUnblock(author.toReference())
}

private fun ProfileAuthor.toReference() = AuthorReference(id, slug, name, avatarUrl)

internal fun publicProfileFeatureScreenRenderer(
    repository: PublicProfileRepository,
    sessions: SessionReader,
    users: UserRepository,
    visibility: AuthorVisibility,
    blocks: AuthorBlockActions,
): PublicProfileFeatureScreenRenderer {
    val checkIns = ProfilePublicCheckIns(repository)
    val slots = profileSlots(checkIns)
    return PublicProfileFeatureScreenRenderer(createPublicProfileEntry(
        data = repository,
        checkIns = checkIns,
        accounts = AppProfileAccountPort(sessions, users),
        visibility = AppProfileAuthorVisibility(visibility),
        authorActions = AppProfileAuthorActions(blocks),
        slots = slots,
    ))
}


/** The legacy-rendered pieces shared by the public profile and the Profile tab. */
internal fun profileSlots(checkIns: ProfilePublicCheckIns): ProfileSlots {
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
        activityPreview = { slug, activity, items, loading, failed, onRetry, onOpen ->
            PublicProfileActivityPreview(slug, activity, items.map { it.occurredAt to it.photoPreviewUrls },
                loading, failed, onRetry, onOpen)
        },
    )
}
