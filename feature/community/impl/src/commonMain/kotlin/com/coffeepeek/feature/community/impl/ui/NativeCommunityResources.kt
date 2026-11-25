@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.impl.resources.*
import org.jetbrains.compose.resources.*

/** Resolves the same feature resources without creating a composition or platform renderer. */
internal class NativeCommunityResources(val language: String) {
    private val environment = getSystemResourceEnvironment()
    suspend fun text(resource: StringResource, vararg args: Any): String =
        if (args.isEmpty()) getString(environment, resource) else getString(environment, resource, *args)

    suspend fun postTexts() = NativePostTexts(
        text(Res.string.community_you), text(Res.string.community_user),
        text(Res.string.community_shop_default), text(Res.string.community_shelf_bag),
        text(Res.string.community_drink_other), text(Res.string.community_details_separator),
        text(Res.string.community_open_coffee), text(Res.string.community_saving),
        text(Res.string.community_publish), text(Res.string.community_label_private),
        text(Res.string.community_label_public), text(Res.string.community_label_rejected),
        text(Res.string.community_label_unknown), text(Res.string.community_rejection_fallback),
        text(Res.string.community_rejection_edit), text(Res.string.community_share),
        text(Res.string.community_edit), text(Res.string.community_hide),
        text(Res.string.community_report), text(Res.string.community_block_author),
        text(Res.string.community_open_post), text(Res.string.community_actions),
    )

    suspend fun postFormatted(post: CommunityPost, own: Boolean, canLike: Boolean, texts: NativePostTexts) = NativePostFormattedTexts(
        text(Res.string.community_open_profile, nativePostAuthorName(post, own, texts)), rating(post),
        post.photos.originals.indices.map { text(Res.string.community_photo, it + 1, post.photos.originals.size) },
        text(nativeHelpfulResource(post, canLike), post.helpfulCount),
        post.home?.doseGrams?.let { text(Res.string.community_dose, it.toString()) },
        post.home?.brewDurationSeconds?.let { text(Res.string.community_seconds, it) },
        post.home?.tasteScore?.let { text(Res.string.community_taste, it) },
    )

    private suspend fun rating(post: CommunityPost): String {
        val rating = post.rating ?: return ""
        return text(Res.string.community_rating_description, rating.coffee, rating.service, rating.place,
            oneDecimal(requireNotNull(nativePostAverage(post))))
    }

    suspend fun feedTexts() = NativeCommunityFeedTexts(
        text(Res.string.community_title), text(Res.string.community_loading),
        text(Res.string.community_error_title), text(Res.string.community_error_body),
        text(Res.string.community_restart), text(Res.string.community_retry),
        text(Res.string.community_empty), text(Res.string.community_sign_in_to_read),
        text(Res.string.community_refresh), text(Res.string.community_saved_title),
        text(Res.string.community_saved_body), text(Res.string.community_saved_open),
    )

    suspend fun detailTexts() = NativePostDetailTexts(
        text(Res.string.community_post_title), text(Res.string.community_back),
        text(Res.string.community_loading), text(Res.string.community_error_title),
        text(Res.string.community_error_body), text(Res.string.community_post_unavailable_title),
        text(Res.string.community_post_unavailable_body), text(Res.string.community_retry),
    )
}
