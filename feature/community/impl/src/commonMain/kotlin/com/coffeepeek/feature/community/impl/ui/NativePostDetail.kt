package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.impl.post.*

internal data class NativePostDetailTexts(val title: String, val back: String, val loading: String,
    val errorTitle: String, val errorBody: String, val unavailableTitle: String, val unavailableBody: String, val retry: String)

internal fun nativePostDetailState(state: PostState, card: CommunityCardPresentation?, texts: NativePostDetailTexts): CommunityPostPresentation {
    val unavailable = state.error == PostError.Unavailable
    val failed = state.error == PostError.Failed
    return CommunityPostPresentation(card, texts.title, texts.back,
        loadingLabel = texts.loading.takeIf { card == null && !unavailable && !failed },
        errorTitle = if (card != null || state.error == null) null else if (unavailable) texts.unavailableTitle else texts.errorTitle,
        errorText = if (card != null || state.error == null) null else if (unavailable) texts.unavailableBody else texts.errorBody,
        retryLabel = texts.retry.takeIf { card == null && failed })
}
