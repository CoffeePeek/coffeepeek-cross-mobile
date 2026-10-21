package com.coffeepeek.feature.profile.domain

data class PublicProfile(
    val slug: String,
    val nickname: String,
    val about: String?,
    val avatarUrl: String?,
    val stats: PublicProfileStats,
    val favorites: List<PublicProfileFavorite>,
    val activity: PublicProfileActivity?,
    val canonicalPath: String? = null,
    val isFollowing: Boolean = false,
    val authorId: String? = null,
    val isBlockedByViewer: Boolean = false,
)

data class PublicProfileStats(
    val checkIns: Int,
    val coffeeShops: Int,
    val roasters: Int,
    val followers: Int,
)

enum class PublicFavoriteKind { CoffeeShop, Roaster, Drink }

data class PublicProfileFavorite(
    val id: String,
    val title: String,
    val kind: PublicFavoriteKind,
    val imageUrl: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val address: String? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val isOpen: Boolean? = null,
    val tags: List<String> = emptyList(),
    val coffeeShopsCount: Int? = null,
    val coffeeProductsCount: Int? = null,
    val availableCoffeeProducts: Int? = null,
    val publicAddress: ProfileAddress? = null,
    val drinkSlug: String? = null,
)

data class PublicProfileFollowState(val isFollowing: Boolean, val followers: Int)

data class PublicProfileActivity(
    /** ISO year and month, independent of the device's display language. */
    val month: String,
    val activeDays: Set<Int>,
    val timeZone: String = "Europe/Minsk",
    val totalCheckIns: Int = 0,
    val checkInCounts: Map<Int, Int> = emptyMap(),
)

enum class PublicProfileConnectionKind { Followers, Following }

data class PublicProfileConnection(
    val userName: String,
    val avatarUrl: String?,
    val slug: String,
    val canonicalPath: String?,
)

data class PublicProfileConnectionsPage(
    val items: List<PublicProfileConnection>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
)

class PublicProfileAuthenticationRequired : Exception("Войдите в аккаунт, чтобы подписаться")
class PublicProfileUnavailable : Exception("Профиль недоступен")

/** Canonical address of a public entity, independent of the app's legacy model. */
data class ProfileAddress(val slug: String, val canonicalPath: String, val revision: Int, val isAlias: Boolean)

interface PublicProfileData {
    suspend fun getProfile(slug: String): Result<PublicProfile>
    suspend fun setFollowing(slug: String, isFollowing: Boolean): Result<PublicProfileFollowState>
    suspend fun getFavorites(slug: String): Result<List<PublicProfileFavorite>>
    suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String = "Europe/Minsk"): Result<PublicProfileActivity>
    suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int = 1, pageSize: Int = 20): Result<PublicProfileConnectionsPage>
}

/**
 * App-supplied enrichment of the viewed user's favorites. Each call may fail or return nothing; the repository then keeps the
 * plain favorite entry, as it always did. Implementations must not fetch the viewer's own favorites.
 */
interface PublicProfileFavoriteDetails {
    suspend fun roasterSummaries(): List<FavoriteRoasterSummary>
    suspend fun shop(slug: String): FavoriteShopDetails?
    suspend fun roaster(slug: String): FavoriteRoasterDetails?
}

data class FavoriteRoasterSummary(
    val slug: String, val photoUrl: String?, val tags: List<String>,
    val coffeeShopsCount: Int, val coffeeProductsCount: Int, val availableCoffeeProducts: Int,
)

data class FavoriteShopDetails(
    val photoUrl: String?, val description: String?, val address: String?, val rating: Double?,
    val reviewCount: Int, val isOpen: Boolean, val tags: List<String>,
)

data class FavoriteRoasterDetails(val photoUrl: String?, val about: String?)

data class PublicProfileCheckIn(
    val id: String,
    val coffeeShopSlug: String?,
    val venue: String,
    val title: String,
    val details: String?,
    val rating: Double?,
    val note: String,
    val occurredAt: String,
    val photoUrls: List<String>,
    /** Small previews for lists; equals [photoUrls] when the server sends no thumbnails. */
    val photoPreviewUrls: List<String> = photoUrls,
)

data class PublicProfileCheckInPage(
    val items: List<PublicProfileCheckIn>,
    val nextCursor: String?,
)

/** Own-profile identity is the public address (slug or canonical path), never the account id or a display name. */
fun isOwnPublicProfile(own: ProfileAddress, profile: PublicProfile): Boolean =
    own.slug.isNotBlank() && own.slug == profile.slug ||
        own.canonicalPath.isNotBlank() && profile.canonicalPath?.trimEnd('/') == own.canonicalPath.trimEnd('/')

private val activityMonthPattern = Regex("\\d{4}-(0[1-9]|1[0-2])")

/** `yyyy-MM` with a year the server accepts. */
fun isValidActivityMonth(month: String): Boolean = month.matches(activityMonthPattern) && month.take(4).toInt() in 2000..2100
