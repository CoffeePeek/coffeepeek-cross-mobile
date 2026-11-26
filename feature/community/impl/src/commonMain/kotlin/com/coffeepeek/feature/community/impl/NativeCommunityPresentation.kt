package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.api.*

/** A single native sheet owns its draft; updates keep the same key and platform controller. */
sealed interface NativeCommunitySheet {
    val key: String
    data class Editor(override val key: String, val data: CommunityEditorPresentation, val actions: CommunityEditorActions) : NativeCommunitySheet
    data class Report(override val key: String, val data: CommunityReportPresentation, val actions: CommunityReportActions) : NativeCommunitySheet
}

class NativeCommunityPhoto(val key: Long, val thumbnails: List<String>, val originals: List<String>, val index: Int,
    val dismiss: () -> Unit)

class NativeCommunityFeedSnapshot(val data: CommunityFeedPresentation, val actions: CommunityFeedActions,
    val sheet: NativeCommunitySheet?, val photo: NativeCommunityPhoto?, val firstPageCount: Int?, val publicTimeline: Boolean)

class NativeCommunityPostSnapshot(val data: CommunityPostPresentation, val actions: CommunityCardActions,
    val sheet: NativeCommunitySheet?, val photo: NativeCommunityPhoto?)
