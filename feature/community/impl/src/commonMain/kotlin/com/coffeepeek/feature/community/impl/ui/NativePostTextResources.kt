package com.coffeepeek.feature.community.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.impl.resources.*

@Composable
internal fun nativePostTexts() = NativePostTexts(
    stringResource(Res.string.community_you), stringResource(Res.string.community_user),
    stringResource(Res.string.community_shop_default), stringResource(Res.string.community_shelf_bag),
    stringResource(Res.string.community_drink_other), stringResource(Res.string.community_details_separator),
    stringResource(Res.string.community_open_coffee), stringResource(Res.string.community_saving),
    stringResource(Res.string.community_publish), stringResource(Res.string.community_label_private),
    stringResource(Res.string.community_label_public), stringResource(Res.string.community_label_rejected),
    stringResource(Res.string.community_label_unknown), stringResource(Res.string.community_rejection_fallback),
    stringResource(Res.string.community_rejection_edit), stringResource(Res.string.community_share),
    stringResource(Res.string.community_edit), stringResource(Res.string.community_hide),
    stringResource(Res.string.community_report), stringResource(Res.string.community_block_author),
    stringResource(Res.string.community_open_post), stringResource(Res.string.community_actions),
)

@Composable
internal fun nativePostFormattedTexts(post: CommunityPost, own: Boolean, canLike: Boolean, texts: NativePostTexts) = NativePostFormattedTexts(
    stringResource(Res.string.community_open_profile, nativePostAuthorName(post, own, texts)), nativePostRatingText(post),
    post.photos.originals.indices.map { stringResource(Res.string.community_photo, it + 1, post.photos.originals.size) },
    stringResource(nativeHelpfulResource(post, canLike), post.helpfulCount),
    post.home?.doseGrams?.let { stringResource(Res.string.community_dose, it.toString()) },
    post.home?.brewDurationSeconds?.let { stringResource(Res.string.community_seconds, it) },
    post.home?.tasteScore?.let { stringResource(Res.string.community_taste, it) },
)

@Composable
private fun nativePostRatingText(post: CommunityPost): String {
    val rating = post.rating ?: return ""
    return stringResource(Res.string.community_rating_description, rating.coffee, rating.service, rating.place,
        oneDecimal(requireNotNull(nativePostAverage(post))))
}

internal fun nativeHelpfulResource(post: CommunityPost, canLike: Boolean) = when {
    !canLike -> Res.string.community_likes
    post.isHelpfulByCurrentUser -> Res.string.community_unlike
    else -> Res.string.community_like
}
