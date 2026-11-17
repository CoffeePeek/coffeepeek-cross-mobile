package com.coffeepeek.feature.community.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.rules.publicationLabel
import com.coffeepeek.feature.community.impl.resources.*

@Composable
internal fun nativePostPresentation(post: CommunityPost, isOwn: Boolean, publicTimeline: Boolean,
    showHelpfulCount: Boolean, enabled: Boolean, changing: Boolean, actions: PostCardActions, host: CommunityHost,
    horizontalInset: Double): CommunityCardPresentation {
    return CommunityCardPresentation(
        key = "${post.isHome}:${post.id}", header = nativePostHeader(post, isOwn, host),
        body = nativePostBody(post, host),
        footer = CommunityCardFooter(post.helpfulCount.takeIf { showHelpfulCount }, post.isHelpfulByCurrentUser,
            helpfulDescription(post.helpfulCount, post.isHelpfulByCurrentUser, actions.onHelpful),
            post.drink?.displayName(Locale.current.language), actions.onPublish?.let {
                stringResource(if (changing) Res.string.community_saving else Res.string.community_publish)
            }),
        status = nativePostStatus(post, isOwn, publicTimeline, actions),
        menu = nativePostMenu(actions, enabled, changing), openLabel = stringResource(Res.string.community_open_post),
        menuLabel = stringResource(Res.string.community_actions),
        horizontalInset = horizontalInset,
    )
}

@Composable
private fun nativePostHeader(post: CommunityPost, isOwn: Boolean, host: CommunityHost): CommunityCardHeader {
    val name = post.author.name.ifBlank { stringResource(if (isOwn) Res.string.community_you else Res.string.community_user) }
    val date = if (post.createdAt.isNotBlank()) host.displayDate(post.createdAt) else ""
    return CommunityCardHeader(name, post.author.avatarUrl, date, stringResource(Res.string.community_open_profile, name))
}

@Composable
private fun nativePostBody(post: CommunityPost, host: CommunityHost): CommunityCardBody {
    val rating = post.rating
    val average = rating?.let { (it.coffee + it.service + it.place) / 3.0 }
    val ratingLabel = if (rating != null && average != null) stringResource(Res.string.community_rating_description,
        rating.coffee, rating.service, rating.place, oneDecimal(average)) else ""
    return CommunityCardBody(post.isHome, post.shopName.ifBlank { stringResource(Res.string.community_shop_default) },
        post.note, average, ratingLabel, nativeBeanName(post), post.home?.roasterName, post.home?.shelfBagCardUrl,
        post.home?.details(host.brewMethodLabel).orEmpty(), nativePostPhotos(post),
        stringResource(Res.string.community_details_separator), stringResource(Res.string.community_open_coffee))
}

@Composable
private fun nativeBeanName(post: CommunityPost): String? = post.home?.beanName?.takeIf(String::isNotBlank)
    ?: if (post.home?.shelfBagCardUrl != null) stringResource(Res.string.community_shelf_bag) else null

@Composable
private fun nativePostPhotos(post: CommunityPost): List<CommunityCardPhoto> = post.photos.originals.mapIndexed { index, original ->
    CommunityCardPhoto(post.photos.thumbnails.getOrNull(index)?.takeIf(String::isNotBlank) ?: original,
        post.photos.inlineSource(index), stringResource(Res.string.community_photo, index + 1, post.photos.originals.size))
}

@Composable
private fun nativePostStatus(post: CommunityPost, isOwn: Boolean, publicTimeline: Boolean, actions: PostCardActions): CommunityCardStatus {
    val rejected = isOwn && post.moderation == PostModeration.Rejected
    return CommunityCardStatus(
        publication = if (!publicTimeline && !rejected) publicationText(post.publicationLabel()) else null,
        rejectedTitle = if (rejected) stringResource(Res.string.community_label_rejected) else null,
        rejectedReason = if (rejected) post.rejectionReason?.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.community_rejection_fallback) else null,
        editLabel = if (rejected && actions.onEdit != null) stringResource(Res.string.community_rejection_edit) else null,
    )
}

@Composable
private fun nativePostMenu(actions: PostCardActions, enabled: Boolean, changing: Boolean): List<CommunityCardMenuItem> = buildList {
    if (actions.onShare != null) add(CommunityCardMenuItem("share", stringResource(Res.string.community_share), enabled))
    if (actions.onEdit != null) add(CommunityCardMenuItem("edit", stringResource(Res.string.community_edit), enabled))
    if (actions.onHide != null) add(CommunityCardMenuItem("hide",
        stringResource(if (changing) Res.string.community_saving else Res.string.community_hide), enabled && !changing))
    if (actions.onReport != null) add(CommunityCardMenuItem("report", stringResource(Res.string.community_report), enabled))
    if (actions.onBlock != null) add(CommunityCardMenuItem("block", stringResource(Res.string.community_block_author), enabled, true))
}

internal fun nativePostActions(actions: PostCardActions, enabled: Boolean, changing: Boolean): CommunityCardActions {
    val callbacks = mapOf("author" to actions.onAuthor, "shop" to actions.onShop, "coffee" to actions.onCoffee,
        "open" to actions.onOpen, "like" to actions.onHelpful,
        "publish" to actions.onPublish.takeIf { enabled }, "edit" to actions.onEdit.takeIf { enabled },
        "hide" to actions.onHide.takeIf { enabled && !changing }, "share" to actions.onShare.takeIf { enabled },
        "report" to actions.onReport.takeIf { enabled }, "block" to actions.onBlock.takeIf { enabled })
    return CommunityCardActions(callbacks.mapNotNull { (key, value) -> value?.let { key to it } }.toMap(), actions.onPhoto)
}
