package com.coffeepeek.admin.di.profile

import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.impl.PublicProfileSnapshot

data class IosPublicProfileState(
    val active: Boolean, val loading: Boolean, val errorKey: String?, val refreshFailed: Boolean,
    val visibilityRevision: Long, val blockedName: String?, val identity: IosOwnProfileIdentity?,
    val slug: String?, val own: Boolean, val signedIn: Boolean, val following: Boolean, val followEnabled: Boolean,
    val followFailed: Boolean, val canShare: Boolean, val activity: IosPublicProfileActivity,
    val checkIns: IosPublicProfileCheckIns, val connections: IosPublicProfileConnections,
)

data class IosPublicProfileActivity(val days: List<IosPublicProfileDay>, val loading: Boolean, val failed: Boolean)
data class IosPublicProfileDay(val date: String, val today: Boolean, val count: Int, val photo: String?)
data class IosPublicProfileCheckIns(
    val rows: List<IosOwnProfileCheckIn>, val loading: Boolean, val failed: Boolean, val more: Boolean,
    val ratings: Map<String, IosPublicProfileRating>,
)
data class IosPublicProfileRating(val coffee: Int, val service: Int, val place: Int)
data class IosPublicProfileConnections(
    val open: Boolean, val followers: Boolean, val loading: Boolean, val failed: Boolean, val more: Boolean,
    val people: List<IosOwnProfileFollower>,
)

internal fun PublicProfileSnapshot.iosState(language: String) = IosPublicProfileState(
    active = active, loading = loading, errorKey = errorKey.takeIf { loadError != null }, refreshFailed = loadError != null,
    visibilityRevision = visibilityRevision, blockedName = profile?.takeIf { it.isBlockedByViewer }?.nickname,
    identity = identity(), slug = profile?.takeUnless { it.isBlockedByViewer }?.slug, own = own, signedIn = signedIn,
    following = following, followEnabled = followEnabled, followFailed = followFailed, canShare = shareUrl() != null,
    activity = IosPublicProfileActivity(week.map { IosPublicProfileDay(it.date, it.today, it.count, it.photo) },
        activityLoading, activityFailed),
    checkIns = IosPublicProfileCheckIns(checkIns.map { it.iosCheckIn(language, profile?.nickname.orEmpty(), profile?.avatarUrl) },
        checkInsLoading, checkInsFailed, hasMoreCheckIns, ratings()),
    connections = IosPublicProfileConnections(connectionKind != null, connectionKind == PublicProfileConnectionKind.Followers,
        connectionsLoading, connectionsFailed, hasMoreConnections,
        connections.map { IosOwnProfileFollower(it.slug, it.userName, it.avatarUrl) }),
)

internal fun PublicProfileSnapshot.shareUrl(): String? = profile?.takeUnless { it.isBlockedByViewer }?.canonicalPath
    ?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { "https://coffeepeek.by$it" }

private fun PublicProfileSnapshot.identity(): IosOwnProfileIdentity? {
    val visible = profile?.takeUnless { it.isBlockedByViewer } ?: return null
    val stats = visible.stats
    return IosOwnProfileIdentity(visible.nickname, visible.about, visible.avatarUrl, stats.checkIns, stats.coffeeShops,
        stats.roasters, stats.followers, canOpenFollowers = true)
}

private fun PublicProfileSnapshot.ratings(): Map<String, IosPublicProfileRating> = checkIns.mapNotNull { row ->
    row.checkIn.scores?.let { row.checkIn.id to IosPublicProfileRating(it.coffee, it.service, it.place) }
}.toMap()
