package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.CommunityFeedPresentation

class IosCommunityFeedState internal constructor(
    data: CommunityFeedPresentation?, cards: List<IosCommunityCard>, val revision: Long,
) {
    val active = data != null
    val sessionGeneration = data?.sessionGeneration ?: 0L
    val title = data?.title.orEmpty()
    val cards = cards.takeIf { active }.orEmpty()
    val teaserKey = data?.teaserKey
    val confirmedIds = data?.confirmedIds.orEmpty()
    val loading = data?.loading == true
    val refreshing = data?.refreshing == true
    val loadingMore = data?.loadingMore == true
    val canLoadMore = data?.canLoadMore == true
    val guest = data?.guest == true
    val interactive = data?.interactive == true
    val loadingLabel = data?.loadingLabel.orEmpty()
    val errorTitle = data?.errorTitle.orEmpty()
    val errorText = data?.errorText.orEmpty()
    val retryLabel = data?.retryLabel.orEmpty()
    val emptyLabel = data?.emptyLabel.orEmpty()
    val signInLabel = data?.signInLabel.orEmpty()
    val refreshLabel = data?.refreshLabel.orEmpty()
    val confirmedTitle = data?.confirmedTitle.orEmpty()
    val confirmedBody = data?.confirmedBody.orEmpty()
    val confirmedOpenLabel = data?.confirmedOpenLabel.orEmpty()
    val bottomClearance = data?.bottomClearance ?: 0.0
}
