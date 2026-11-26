package com.coffeepeek.feature.community.api

/** Full-screen chrome/status; the card and feature actions are shared with the feed. */
data class CommunityPostPresentation(
    val card: CommunityCardPresentation?, val title: String, val backLabel: String,
    val loadingLabel: String?, val errorTitle: String?, val errorText: String?, val retryLabel: String?,
)
