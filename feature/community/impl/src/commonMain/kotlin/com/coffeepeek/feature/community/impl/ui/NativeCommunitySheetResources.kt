package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.api.CommunityEditorRating
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.EditError
import com.coffeepeek.feature.community.impl.timeline.PostEditState

internal suspend fun NativeCommunityResources.editorTexts() = NativeEditorTexts(
    text(Res.string.community_edit_title), text(Res.string.community_shop_default),
    text(Res.string.community_edit_kept), text(Res.string.community_ratings_title),
    text(Res.string.community_ratings_hint), text(Res.string.community_note_label),
    text(Res.string.community_note_placeholder), text(Res.string.community_drink_label),
    text(Res.string.community_drink_none), text(Res.string.community_drink_other),
    text(Res.string.community_drink_custom), text(Res.string.community_drink_error),
    text(Res.string.community_drink_retry), text(Res.string.community_saving),
    text(Res.string.community_save), text(Res.string.community_cancel),
)

internal suspend fun NativeCommunityResources.editorRatings(edit: PostEditState) = nativeEditorAxes(edit).map { (key, value) ->
    val label = text(nativeEditorRatingResource(key))
    CommunityEditorRating(key, label, value, Res.getUri("drawable/community_rating_$key.png"),
        (1..5).map { text(Res.string.community_choose_rating, label, it) })
}

internal suspend fun NativeCommunityResources.editError(error: EditError?): String? = when (error) {
    null -> null
    is EditError.Server -> error.message ?: text(Res.string.community_edit_save_failed)
    is EditError.Invalid -> text(nativeEditIssueResource(error.issue))
}

internal suspend fun NativeCommunityResources.reportTexts() = NativeReportTexts(
    text(Res.string.community_report_title), text(Res.string.community_report_placeholder),
    text(Res.string.community_report_issue), text(Res.string.community_report_failed),
    text(Res.string.community_report_sending), text(Res.string.community_report_send), text(Res.string.community_cancel),
)

internal suspend fun NativeCommunityResources.messageTexts() = MessageTexts(
    text(Res.string.community_vote_failed), text(Res.string.community_visibility_failed),
    text(Res.string.community_report_sent), text(Res.string.community_edit_saved),
)
