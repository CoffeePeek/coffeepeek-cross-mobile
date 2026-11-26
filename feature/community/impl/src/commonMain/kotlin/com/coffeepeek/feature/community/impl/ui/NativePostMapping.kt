package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.rules.PublicationLabel
import com.coffeepeek.feature.community.domain.rules.publicationLabel

/** Resolved copy only. The mapper has no resource, rendering or platform lifetime. */
internal data class NativePostTexts(
    val you: String, val user: String, val shopDefault: String, val shelfBag: String,
    val other: String, val separator: String, val openCoffee: String,
    val saving: String, val publish: String, val privateLabel: String, val publicLabel: String,
    val rejected: String, val unknown: String, val rejectionFallback: String, val rejectionEdit: String,
    val share: String, val edit: String, val hide: String, val report: String, val block: String,
    val open: String, val actions: String,
)

internal data class NativePostFormattedTexts(
    val openAuthor: String, val rating: String, val photos: List<String>, val likes: String,
    val dose: String?, val duration: String?, val taste: String?,
)

internal fun nativePostAuthorName(post: CommunityPost, own: Boolean, texts: NativePostTexts): String =
    post.author.name.ifBlank { if (own) texts.you else texts.user }

internal fun nativePostAverage(post: CommunityPost): Double? = post.rating?.let { (it.coffee + it.service + it.place) / 3.0 }

internal fun nativePostPresentation(post: CommunityPost, own: Boolean, publicTimeline: Boolean,
    showHelpfulCount: Boolean, enabled: Boolean, changing: Boolean, actions: PostCardActions,
    texts: NativePostTexts, formatted: NativePostFormattedTexts, date: String, language: String,
    methodLabel: (String) -> String?, horizontalInset: Double,
): CommunityCardPresentation = CommunityCardPresentation(
    key = "${post.isHome}:${post.id}",
    header = CommunityCardHeader(nativePostAuthorName(post, own, texts), post.author.avatarUrl, date, formatted.openAuthor),
    body = nativePostBody(post, texts, formatted, methodLabel),
    footer = CommunityCardFooter(post.helpfulCount.takeIf { showHelpfulCount }, post.isHelpfulByCurrentUser,
        formatted.likes, post.drink?.displayName(language), actions.onPublish?.let { if (changing) texts.saving else texts.publish }),
    status = nativePostStatus(post, own, publicTimeline, actions, texts),
    menu = nativePostMenu(actions, enabled, changing, texts), openLabel = texts.open,
    menuLabel = texts.actions, horizontalInset = horizontalInset,
)

private fun nativePostBody(post: CommunityPost, texts: NativePostTexts, formatted: NativePostFormattedTexts,
    methodLabel: (String) -> String?): CommunityCardBody = CommunityCardBody(
    post.isHome, post.shopName.ifBlank { texts.shopDefault }, post.note, nativePostAverage(post), formatted.rating,
    post.home?.beanName?.takeIf(String::isNotBlank) ?: texts.shelfBag.takeIf { post.home?.shelfBagCardUrl != null },
    post.home?.roasterName, post.home?.shelfBagCardUrl, nativePostRecipe(post, texts, formatted, methodLabel),
    nativePostPhotos(post, formatted.photos), texts.separator, texts.openCoffee,
)

private fun nativePostRecipe(post: CommunityPost, texts: NativePostTexts, formatted: NativePostFormattedTexts,
    methodLabel: (String) -> String?): String {
    val home = post.home ?: return ""
    val code = home.brewMethodCode
    val method = code?.let(methodLabel)
    val generic = code == "other" && (method == null || method == code)
    return listOfNotNull(if (generic) texts.other else method, formatted.dose, formatted.duration, formatted.taste)
        .joinToString(texts.separator)
}

private fun nativePostPhotos(post: CommunityPost, labels: List<String>): List<CommunityCardPhoto> =
    post.photos.originals.mapIndexed { index, original ->
        CommunityCardPhoto(post.photos.thumbnails.getOrNull(index)?.takeIf(String::isNotBlank) ?: original,
            post.photos.inlineSource(index), labels[index])
    }

private fun nativePostStatus(post: CommunityPost, own: Boolean, publicTimeline: Boolean,
    actions: PostCardActions, texts: NativePostTexts): CommunityCardStatus {
    val rejected = own && post.moderation == PostModeration.Rejected
    return CommunityCardStatus(
        publication = if (!publicTimeline && !rejected) nativePublicationText(post.publicationLabel(), texts) else null,
        rejectedTitle = texts.rejected.takeIf { rejected },
        rejectedReason = if (rejected) post.rejectionReason?.takeIf(String::isNotBlank) ?: texts.rejectionFallback else null,
        editLabel = texts.rejectionEdit.takeIf { rejected && actions.onEdit != null },
    )
}

private fun nativePublicationText(label: PublicationLabel, texts: NativePostTexts) = when (label) {
    PublicationLabel.Private -> texts.privateLabel
    PublicationLabel.Public -> texts.publicLabel
    PublicationLabel.Rejected -> texts.rejected
    PublicationLabel.Unknown -> texts.unknown
}

private fun nativePostMenu(actions: PostCardActions, enabled: Boolean, changing: Boolean, texts: NativePostTexts) = buildList {
    if (actions.onShare != null) add(CommunityCardMenuItem("share", texts.share, enabled))
    if (actions.onEdit != null) add(CommunityCardMenuItem("edit", texts.edit, enabled))
    if (actions.onHide != null) add(CommunityCardMenuItem("hide", if (changing) texts.saving else texts.hide, enabled && !changing))
    if (actions.onReport != null) add(CommunityCardMenuItem("report", texts.report, enabled))
    if (actions.onBlock != null) add(CommunityCardMenuItem("block", texts.block, enabled, true))
}
