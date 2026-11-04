package com.coffeepeek.feature.profile.api

import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn

/** Render-only data. Photo URLs may be retained, but photo bytes and credentials are never stored here. */
data class ProfileSnapshot(
    val profile: PublicProfile,
    val checkIns: List<PublicProfileCheckIn>,
    val nextCheckInCursor: String?,
    val activityMonth: String,
)

/** A short-lived, account- and visibility-scoped presentation cache; network requests still refresh every entry. */
interface ProfileSnapshotStore {
    fun read(scope: String, slug: String, visibilityRevision: Long): ProfileSnapshot?
    fun write(scope: String, slug: String, visibilityRevision: Long, snapshot: ProfileSnapshot)
    fun clear()

    object None : ProfileSnapshotStore {
        override fun read(scope: String, slug: String, visibilityRevision: Long): ProfileSnapshot? = null
        override fun write(scope: String, slug: String, visibilityRevision: Long, snapshot: ProfileSnapshot) = Unit
        override fun clear() = Unit
    }
}
