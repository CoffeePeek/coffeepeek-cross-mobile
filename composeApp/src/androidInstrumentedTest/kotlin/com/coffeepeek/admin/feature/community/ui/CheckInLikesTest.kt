package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInVisibility
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CheckInLikesTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun ownPublicationOffersBothLikeAndUnlikeActions() {
        val post = mutableStateOf(CheckIn("own", "shop", "Кофейня", "Кофе", "2026-10-20T09:00:00Z",
            visibility = CheckInVisibility.Public))
        var votes = 0
        compose.setContent {
            CoffeePeekTheme {
                TimelineCheckInCard(post.value, isOwn = true, isPublicTimeline = true, canAct = true,
                    changingVisibility = false, onShopClick = {}, onEdit = {}, onVisibility = {},
                    onHelpful = { votes++ }, onReport = {}, onPhotoClick = { _, _ -> })
            }
        }
        compose.onNodeWithContentDescription("Поставить лайк, лайков: 0").assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(1, votes)
            post.value = post.value.copy(isHelpfulByCurrentUser = true, helpfulCount = 8)
        }
        compose.onNodeWithContentDescription("Убрать лайк, лайков: 8").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(2, votes) }
    }
}
