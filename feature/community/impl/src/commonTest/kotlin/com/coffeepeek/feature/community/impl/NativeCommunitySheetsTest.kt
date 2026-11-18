package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.model.PostDrink
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.impl.post.PostAction
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.PostEditState
import com.coffeepeek.feature.community.impl.ui.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NativeCommunitySheetsTest {
    @Test fun editorKeepsSavedNameEvenAfterCatalogRenamesOrRemovesDrink() {
        val edit = PostEditState(post().copy(drink = PostDrink("filter", null, "Сохранённый", "Saved")))
        val renamed = nativeEditorDrinks(edit, listOf(DrinkOption("filter", "Новый", "New")), "ru", "Нет", "Другое")
        val missing = nativeEditorDrinks(edit, emptyList(), "en", "None", "Other")
        assertEquals("Сохранённый", renamed.single { it.key == "drink:filter" }.label)
        assertEquals(listOf("none", "drink:filter"), missing.map { it.key })
        assertEquals("Saved", missing.last().label)
    }

    @Test fun nativeRatingKeysChangeOnlyTheirOwnAxisAndIgnoreInvalidRatings() {
        val actions = mutableListOf<CommunityAction>()
        val edit = PostEditState(post(), rating = PostRating(place = 3, service = 4, coffee = 2))
        val native = nativeEditorActions(edit, emptyList(), timelineEditorCallbacks(actions::add))
        native.rating("coffee", 5)
        native.rating("service", 1)
        native.rating("atmosphere", 4)
        native.rating("coffee", 0)
        native.rating("unknown", 3)
        assertEquals(listOf(PostRating(3, 4, 5), PostRating(3, 1, 2), PostRating(4, 4, 2)),
            actions.filterIsInstance<CommunityAction.EditRating>().map { it.rating })
    }

    @Test fun drinkChoiceIsCatalogBoundAndOtherStartsEmptyWhileSavedSelectionStaysUntouched() {
        val actions = mutableListOf<CommunityAction>()
        val edit = PostEditState(post(), drinkSlug = "saved")
        val native = nativeEditorActions(edit, listOf(DrinkOption("other", "Другое", "Other")), timelineEditorCallbacks(actions::add))
        native.drink("drink:injected")
        native.drink("drink:saved")
        native.drink("drink:other")
        native.drink("none")
        assertEquals(listOf(CommunityAction.EditDrink("other", ""), CommunityAction.EditDrink(null, null)), actions)
    }

    @Test fun customNameIsLimitedAndOnlyOtherAcceptsIt() {
        val actions = mutableListOf<PostAction>()
        val edit = PostEditState(post(), drinkSlug = "other")
        nativeEditorActions(edit, emptyList(), postEditorCallbacks(actions::add)).customDrink("x".repeat(120))
        nativeEditorActions(edit.copy(drinkSlug = null), emptyList(), postEditorCallbacks(actions::add)).customDrink("ignored")
        assertEquals(PostAction.EditDrink("other", "x".repeat(100)), actions.single())
    }

    @Test fun feedAndStandalonePostRetainDifferentOwnersForAllEditorCommands() {
        val feed = mutableListOf<CommunityAction>()
        val detailActions = mutableListOf<PostAction>()
        val edit = PostEditState(post())
        val timeline = nativeEditorActions(edit, emptyList(), timelineEditorCallbacks(feed::add))
        val detail = nativeEditorActions(edit, emptyList(), postEditorCallbacks(detailActions::add))
        timeline.text("feed")
        timeline.retry()
        timeline.submit()
        timeline.dismiss()
        detail.text("detail")
        detail.retry()
        detail.submit()
        detail.dismiss()
        assertEquals(listOf(CommunityAction.EditText("feed"), CommunityAction.LoadDrinks, CommunityAction.SaveEdit, CommunityAction.DismissEdit), feed)
        assertEquals(listOf(PostAction.EditText("detail"), PostAction.LoadDrinks, PostAction.SaveEdit, PostAction.DismissEdit), detailActions)
    }

    @Test fun reportDoesNotBorrowTheOtherOwnersDraftAndRetainsLengthLimit() {
        val actions = mutableListOf<CommunityAction>()
        val native = nativeReportActions(reportCallbacks(actions::add))
        native.text("x".repeat(2100))
        native.submit()
        native.dismiss()
        assertEquals(2000, assertIs<CommunityAction.ReportText>(actions.first()).text.length)
        assertEquals(listOf(CommunityAction.SubmitReport, CommunityAction.DismissReport), actions.drop(1))
    }
}
