package com.coffeepeek.admin.feature.authorblocking

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekActionSheet
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.admin.feature.community.ui.TimelineCheckInCard
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.PublicAddress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthorBlockingMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reportAndBlockAreSeparateAndBlockUsesAuthorIdentity() {
        val author = AuthorReference("stable-id", "slug", "Автор")
        var blocked: AuthorReference? = null
        var reports = 0
        val actions = object : AuthorBlockActions {
            override fun requestBlock(author: AuthorReference) { blocked = author }
            override fun requestUnblock(author: AuthorReference) = Unit
        }
        compose.setContent { CoffeePeekTheme { CheckInActions(null, { reports++ }, author = author, blockActions = actions) } }
        compose.onNodeWithContentDescription("Действия с чекином").performClick()
        compose.onNodeWithText("Пожаловаться").assertIsDisplayed()
        compose.onNodeWithText("Заблокировать автора").performClick()
        compose.runOnIdle { assertEquals(author, blocked); assertEquals(0, reports) }
    }

    @Test fun ownPostHasEditAndHideWithoutBlock() {
        compose.setContent { CoffeePeekTheme { CheckInActions({}, null, onHideClick = {}) } }
        compose.onNodeWithContentDescription("Действия с чекином").performClick()
        compose.onNodeWithText("Редактировать").assertIsDisplayed()
        compose.onNodeWithText("Скрыть").assertIsDisplayed()
        compose.onNodeWithText("Заблокировать автора").assertDoesNotExist()
        compose.onNodeWithText("Пожаловаться").assertDoesNotExist()
    }

    @Test fun publicTimelineWithUnknownViewerIdentityOffersReportWithoutBlock() {
        val post = CheckIn("post", "shop", "Coffee", "Note", "2026-10-18T09:15:00Z",
            username = "Автор", authorId = "public-author",
            authorAddress = PublicAddress("author", "/users/author", 1, false), visibility = CheckInVisibility.Public)
        compose.setContent {
            CoffeePeekTheme {
                TimelineCheckInCard(post, isOwn = false, isPublicTimeline = true, canAct = true,
                    changingVisibility = false, onShopClick = {}, onEdit = {}, onVisibility = {},
                    onHelpful = {}, onReport = {}, onPhotoClick = { _, _ -> }, canBlockAuthor = false)
            }
        }
        compose.onNodeWithContentDescription("Действия с чекином").performClick()
        compose.onNodeWithText("Пожаловаться").assertIsDisplayed()
        compose.onNodeWithText("Заблокировать автора").assertDoesNotExist()
        compose.onNodeWithText("Редактировать").assertDoesNotExist()
    }
    @Test fun largeTextKeepsConfirmationAndCancelSeparateAndReachable() {
        var confirmed = 0
        var canceled = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                CoffeePeekTheme {
                    CoffeePeekActionSheet("Заблокировать автора?", "Публикации автора будут скрыты. Автор не получит уведомление.",
                        listOf(CoffeePeekDialogAction("Заблокировать", destructive = true, onClick = { confirmed++ })),
                        onDismiss = { canceled++ }, cancelLabel = "Отмена")
                }
            }
        }
        val confirm = compose.onNodeWithText("Заблокировать")
        val cancel = compose.onNodeWithText("Отмена")
        confirm.assertIsDisplayed().assertHeightIsAtLeast(44.dp)
        cancel.assertIsDisplayed().assertHeightIsAtLeast(44.dp)
        assertTrue(cancel.fetchSemanticsNode().boundsInRoot.top > confirm.fetchSemanticsNode().boundsInRoot.bottom)
        cancel.performClick()
        compose.runOnIdle { assertEquals(0, confirmed); assertEquals(1, canceled) }
    }

}
