package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.feature.checkinhistory.impl.ProfileActivitySnapshot
import com.coffeepeek.feature.profile.impl.OwnProfileCheckInSnapshot
import com.coffeepeek.feature.profile.impl.OwnProfileSnapshot

data class IosOwnProfileState(
    val active: Boolean,
    val identity: IosOwnProfileIdentity?,
    val loading: Boolean,
    val refreshing: Boolean,
    val failed: Boolean,
    val refreshFailed: Boolean,
    val checkInsLoading: Boolean,
    val checkInsFailed: Boolean,
    val checkIns: List<IosOwnProfileCheckIn>,
    val connections: IosPublicProfileConnections,
    val moreCheckIns: Boolean = false,
    val loadingMoreCheckIns: Boolean = false,
    val moreCheckInsFailed: Boolean = false,
    val selectedDate: String? = null,
)

data class IosOwnProfileIdentity(
    val name: String,
    val about: String?,
    val avatar: String?,
    val checkIns: Int,
    val shops: Int,
    val roasters: Int,
    val followers: Int,
    val canOpenFollowers: Boolean,
    val following: Int? = null,
)

data class IosOwnProfileCheckIn(
    val id: String, val name: String, val avatar: String?, val shop: String, val shopSlug: String?,
    val home: Boolean, val note: String, val date: String, val drink: String?, val helpfulCount: Int,
    val liked: Boolean, val score: String?, val photos: List<IosOwnProfilePhoto>, val brew: IosOwnProfileBrew?,
)

data class IosOwnProfilePhoto(val url: String?, val canOpen: Boolean, val fallbackUrl: String?)

data class IosOwnProfileBrew(
    val bean: String?, val roaster: String?, val method: String?, val dose: String?,
    val seconds: Int, val taste: Int, val cover: String?, val coffee: String?,
)

data class IosOwnProfileFollower(val slug: String, val name: String, val avatar: String?)

data class IosOwnProfileActivity(
    val expanded: Boolean, val year: Int, val month: Int, val loading: Boolean, val failed: Boolean,
    val selected: String?, val canGoNext: Boolean, val canGoPrevious: Boolean,
    val days: List<IosOwnProfileDay>,
)

data class IosOwnProfileDay(
    val date: String?, val photo: String?, val shopCount: Long, val homeCount: Int,
    val selected: Boolean, val today: Boolean, val enabled: Boolean,
)

internal fun OwnProfileSnapshot.iosState(): IosOwnProfileState {
    return IosOwnProfileState(active, iosIdentity(), loading || publicLoading && !hasContent, refreshing,
        failed || publicFailed && !hasContent, refreshFailed || publicFailed && hasContent,
        false, false, emptyList(),
        IosPublicProfileConnections(connectionsOpen,
            connectionKind != com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind.Following,
            connectionsLoading, connectionsFailed, moreConnections,
            connections.map { IosOwnProfileFollower(it.slug, it.userName, it.avatarUrl) }))
}

private fun OwnProfileSnapshot.iosIdentity(): IosOwnProfileIdentity? {
    val own = identity ?: return null
    val public = publicProfile
    return IosOwnProfileIdentity(displayName.orEmpty(), public?.about ?: own.about,
        public?.avatarUrl ?: own.avatarUrl, public?.stats?.checkIns ?: own.checkInCount,
        public?.stats?.coffeeShops ?: own.addedShopsCount, public?.stats?.roasters ?: own.addedRoastersCount,
        public?.stats?.followers ?: own.followersCount, public != null, followingCount)
}

internal fun OwnProfileCheckInSnapshot.iosCheckIn(
    language: String, fallbackName: String, fallbackAvatar: String?,
): IosOwnProfileCheckIn {
    val row = checkIn
    val name = row.authorName.ifBlank { fallbackName }
    val avatar = row.authorAvatarUrl ?: fallbackAvatar
    return IosOwnProfileCheckIn(row.id, name, avatar, row.venue, row.coffeeShopSlug, row.isHome, row.note,
        utcIsoToLocalDateTime(row.createdAt), drinkName(language), row.helpfulCount, row.isHelpfulByCurrentUser,
        row.scores?.let { (kotlin.math.round(it.average * 10) / 10).toString() },
        originalPhotos.indices.map { IosOwnProfilePhoto(displayPhoto(it), canOpenPhoto(it), fallbackPhoto(it)) }, brew())
}

private fun OwnProfileCheckInSnapshot.brew(): IosOwnProfileBrew? = checkIn.preparation?.let { value ->
    IosOwnProfileBrew(value.beanName, value.roasterName, value.brewMethodCode, value.doseGrams?.toString(),
        value.brewDurationSeconds ?: -1, value.tasteScore ?: -1, value.shelfBagCardUrl, value.coffeeProductSlug)
}

internal fun ProfileActivitySnapshot.iosActivity(expanded: Boolean) = IosOwnProfileActivity(expanded, year, month, loading,
    failed, selected, canGoNext, canGoPrevious,
    days.map { IosOwnProfileDay(it.date, it.photo, it.shopCount, it.homeCount, it.selected, it.today, it.enabled) })
