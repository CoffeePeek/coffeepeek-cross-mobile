package com.coffeepeek.feature.community.api

/** Native list rendering only; the shared feed remains the owner of paging, permissions and actions. */
data class CommunityFeedPresentation(
    val sessionGeneration: Long,
    val title: String,
    val cards: List<CommunityCardPresentation>,
    val teaserKey: String?,
    val confirmedIds: List<String>,
    val loading: Boolean,
    val refreshing: Boolean,
    val loadingMore: Boolean,
    val canLoadMore: Boolean,
    val guest: Boolean,
    val interactive: Boolean,
    val loadingLabel: String,
    val errorTitle: String?,
    val errorText: String?,
    val retryLabel: String,
    val emptyLabel: String,
    val signInLabel: String,
    val refreshLabel: String,
    val confirmedTitle: String,
    val confirmedBody: String,
    val confirmedOpenLabel: String,
    val bottomClearance: Double,
    val accountEpoch: Long,
)

class CommunityFeedActions(
    val actions: Map<String, () -> Unit>,
    val cards: Map<String, CommunityCardActions>,
)
