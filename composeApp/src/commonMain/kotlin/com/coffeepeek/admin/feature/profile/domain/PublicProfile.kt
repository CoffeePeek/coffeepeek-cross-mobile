package com.coffeepeek.admin.feature.profile.domain

import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.CheckIn

internal data class PublicProfile(
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

internal data class PublicProfileStats(
    val checkIns: Int,
    val coffeeShops: Int,
    val roasters: Int,
    val followers: Int,
)

internal enum class PublicFavoriteKind { CoffeeShop, Roaster, Drink }

internal data class PublicProfileFavorite(
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
    val publicAddress: PublicAddress? = null,
    val drinkSlug: String? = null,
)

internal data class PublicProfileFollowState(val isFollowing: Boolean, val followers: Int)

internal data class PublicProfileActivity(
    /** ISO year and month, independent of the device's display language. */
    val month: String,
    val activeDays: Set<Int>,
    val timeZone: String = "Europe/Minsk",
    val totalCheckIns: Int = 0,
    val checkInCounts: Map<Int, Int> = emptyMap(),
)

internal enum class PublicProfileConnectionKind { Followers, Following }

internal data class PublicProfileConnection(
    val userName: String,
    val avatarUrl: String?,
    val slug: String,
    val canonicalPath: String?,
)

internal data class PublicProfileConnectionsPage(
    val items: List<PublicProfileConnection>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
)

internal class PublicProfileAuthenticationRequired : Exception("Войдите в аккаунт, чтобы подписаться")
internal class PublicProfileUnavailable : Exception("Профиль недоступен")

internal data class PublicProfileCheckIn(
    val id: String,
    val coffeeShopSlug: String?,
    val venue: String,
    val title: String,
    val details: String?,
    val rating: Double?,
    val note: String,
    val occurredAt: String,
    val photoUrls: List<String>,
    val source: CheckIn? = null,
)

internal data class PublicProfileCheckInPage(
    val items: List<PublicProfileCheckIn>,
    val nextCursor: String?,
)

internal interface PublicProfileRepository {
    suspend fun getProfile(slug: String): Result<PublicProfile>
    suspend fun getCheckIns(slug: String, cursor: String? = null): Result<PublicProfileCheckInPage>
    suspend fun setFollowing(slug: String, isFollowing: Boolean): Result<PublicProfileFollowState>
    suspend fun getFavorites(slug: String): Result<List<PublicProfileFavorite>>
    suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String = "Europe/Minsk"): Result<PublicProfileActivity>
    suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int = 1, pageSize: Int = 20): Result<PublicProfileConnectionsPage>
}
