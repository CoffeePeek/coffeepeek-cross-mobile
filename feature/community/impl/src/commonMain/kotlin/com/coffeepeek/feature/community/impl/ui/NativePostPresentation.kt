package com.coffeepeek.feature.community.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.CommunityPost

/** Android/temporary host adapter; both entry paths use the ordinary mapper. */
@Composable
internal fun nativePostPresentation(post: CommunityPost, isOwn: Boolean, publicTimeline: Boolean,
    showHelpfulCount: Boolean, enabled: Boolean, changing: Boolean, actions: PostCardActions, host: CommunityHost,
    horizontalInset: Double): CommunityCardPresentation {
    val texts = nativePostTexts()
    val formatted = nativePostFormattedTexts(post, isOwn, actions.onHelpful != null, texts)
    val date = if (post.createdAt.isNotBlank()) host.displayDate(post.createdAt) else ""
    return nativePostPresentation(post, isOwn, publicTimeline, showHelpfulCount, enabled, changing, actions,
        texts, formatted, date, Locale.current.language, host.brewMethodLabel, horizontalInset)
}

internal fun nativePostActions(actions: PostCardActions, enabled: Boolean, changing: Boolean): CommunityCardActions {
    val callbacks = mapOf("author" to actions.onAuthor, "shop" to actions.onShop, "coffee" to actions.onCoffee,
        "open" to actions.onOpen, "like" to actions.onHelpful,
        "publish" to actions.onPublish.takeIf { enabled }, "edit" to actions.onEdit.takeIf { enabled },
        "hide" to actions.onHide.takeIf { enabled && !changing }, "share" to actions.onShare.takeIf { enabled },
        "report" to actions.onReport.takeIf { enabled }, "block" to actions.onBlock.takeIf { enabled })
    return CommunityCardActions(callbacks.mapNotNull { (key, value) -> value?.let { key to it } }.toMap(), actions.onPhoto)
}
