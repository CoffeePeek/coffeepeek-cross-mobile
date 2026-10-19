package com.coffeepeek.feature.shop.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInCard
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopDetailTopActions
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPublicCheckInsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopFeaturesSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopMenuSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopContactSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCoffeeDetailsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopScheduleSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInHelpfulButton
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.domain.model.ScheduleInterval
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.domain.model.ShopMenu
import com.coffeepeek.feature.shop.domain.model.ShopContact
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopRoaster
import com.coffeepeek.feature.shop.impl.ui.compose.component.previewCheckIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Authored for the completion gate: fake content only, no app DI/backend/picker. */
class ShopDetailPresentationContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = false, content: @Composable () -> Unit): ActivityScenario<ShopFormTestActivity> =
        ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                    Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) { content() }
                }
            } }
        }

    @Test fun guestScrollNeverComposesHiddenAuthorOrNote() {
        render {
            ShopPublicCheckInsSection(listOf(previewCheckIn().copy(note = "Visible visit", rating = null),
                previewCheckIn().copy(id = "hidden", username = "Hidden author", note = "Hidden visit", rating = null)),
                "Кофейня", false, null, emptySet(), null, { _, _ -> }, {}, {}, {}, {}, {})
        }.use {
            compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(1)
            compose.onNodeWithText("Hidden author", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Hidden visit", useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test fun ownVisitHasNeitherReportMenuNorInteractiveHelpfulAction() {
        render(dark = true) {
            ShopCheckInCard(previewCheckIn(), { _, _ -> }, showAuthor = true, showHelpful = true,
                isOwn = true, onVote = { error("Own vote must not be interactive") },
                onReport = { error("Own report must not be offered") })
        }.use {
            compose.onNodeWithContentDescription("Действия с чекином").assertDoesNotExist()
            compose.onNodeWithContentDescription("Поставить лайк, лайков: 3").assertDoesNotExist()
        }
    }

    @Test fun anotherUsersHelpfulToggleAndReportMenuForwardCallbacks() {
        var votes = 0
        var reports = 0
        render {
            ShopCheckInCard(previewCheckIn().copy(drinkNameRu = null), { _, _ -> }, showAuthor = true,
                showHelpful = true, onVote = { votes++ }, onReport = { reports++ })
        }.use {
            compose.onNodeWithContentDescription("Поставить лайк, лайков: 3").performClick()
            compose.onNodeWithContentDescription("Действия с чекином").performClick()
            compose.onNodeWithText("Пожаловаться").performClick()
            compose.runOnIdle { assertEquals(1, votes); assertEquals(1, reports) }
        }
    }

    @Test fun savedDrinkIsStaticAndBottomAlignedWithHelpfulCountInBothThemes() {
        listOf(false, true).forEach { dark ->
            render(dark) {
                ShopCheckInCard(previewCheckIn(), { _, _ -> }, showAuthor = true,
                    showHelpful = true, onVote = {})
            }.use {
                val drink = compose.onNodeWithText("Фильтр").assertHasNoClickAction().fetchSemanticsNode().boundsInRoot
                val note = compose.onNodeWithText("Вкусный кофе и приятная атмосфера.").fetchSemanticsNode().boundsInRoot
                val helpful = compose.onNodeWithContentDescription("Поставить лайк, лайков: 3")
                    .fetchSemanticsNode().boundsInRoot
                assertTrue("Drink belongs below the note", drink.top > note.bottom)
                assertTrue("Drink belongs on the trailing side of the footer", drink.left > helpful.right)
                assertTrue("Helpful count and drink share a footer", drink.center.y in helpful.top..helpful.bottom)
            }
        }
    }

    @Test fun hoursHeaderStillExpandsAndCollapsesWithAccessibleActions() {
        render {
            var expanded by remember { mutableStateOf(false) }
            ShopScheduleSection(listOf(ShopSchedule(1, false, listOf(ScheduleInterval("09:00", "21:00")))),
                todayDayOfWeek = 1, expanded = expanded, onToggle = { expanded = !expanded },
                isOpen = true, minuteOfDay = 720)
        }.use {
            compose.onNodeWithText("Понедельник").assertDoesNotExist()
            compose.onNodeWithText("Открыто до 21:00").performClick()
            compose.onNodeWithText("Понедельник").assertIsDisplayed()
            compose.onNodeWithText("Открыто до 21:00").performClick()
            compose.onNodeWithText("Понедельник").assertDoesNotExist()
        }
    }

    @Test fun onlyTheHeartIsInteractiveNotTheAdjacentCount() {
        var clicks = 0
        render {
            var selected by remember { mutableStateOf(false) }
            ShopCheckInHelpfulButton(3, selected, { clicks++; selected = !selected })
        }.use {
            compose.onNodeWithText("3").assertHasNoClickAction().performTouchInput { click() }
            compose.runOnIdle { assertEquals(0, clicks) }
            compose.onNodeWithContentDescription("Поставить лайк, лайков: 3").performClick()
            compose.onNodeWithContentDescription("Убрать лайк, лайков: 3").assertExists()
            compose.runOnIdle { assertEquals(1, clicks) }
        }
    }

    @Test fun backIsAvailableWhenShopHasNotLoadedYet() {
        var back = 0
        render { ShopDetailTopActions(false, false, false, false, { back++ }, {}, {}, {}) }
            .use {
                compose.onNodeWithContentDescription("Назад").performClick()
                compose.onNodeWithContentDescription("Поделиться").assertDoesNotExist()
                compose.runOnIdle { assertEquals(1, back) }
            }
    }

    @Test fun featuresInitiallyShowFiveAndExpansionCanBeReversed() {
        render {
            var expanded by remember { mutableStateOf(false) }
            ShopFeaturesSection((1..6).map { ShopFeature("Feature $it", null, false) },
                expanded, { expanded = !expanded })
        }.use {
            compose.onNodeWithText("Feature 5").assertExists()
            compose.onNodeWithText("Feature 6").assertDoesNotExist()
            compose.onNodeWithText("Показать все 6 особенностей").performClick()
            compose.onNodeWithText("Feature 6").assertExists()
            compose.onNodeWithText("Свернуть").performClick()
            compose.onNodeWithText("Feature 6").assertDoesNotExist()
        }
    }

    @Test fun menuGalleryAndPhotoViewerHaveSeparateCallbacksAndUseInjectedDates() {
        var galleries = 0
        var selectedPhoto: Int? = null
        render {
            ShopMenuSection(ShopMenu(capturedAtUtc = "raw-date", photos = listOf(MenuPhoto("photo", "", ""))),
                onOpenPhotos = { galleries++ }, onOpenPhoto = { selectedPhoto = it },
                formatDate = { "03.10.2026" })
        }.use {
            compose.onNodeWithText("Актуально на 03.10.2026").assertExists()
            compose.onNodeWithText("Фото меню: 1").performClick()
            compose.onNodeWithContentDescription("Фотография меню 1 из 1").performClick()
            compose.runOnIdle { assertEquals(1, galleries); assertEquals(0, selectedPhoto) }
        }
    }

    @Test fun phoneCopyDoesNotOpenTheExternalDialer() {
        var opened: String? = null
        var copied: String? = null
        render {
            ShopContactSection(ShopContact(phone = "+375 29 123 45 67", instagram = "@coffee"),
                onOpenLink = { opened = it }, onCopyPhone = { copied = it })
        }.use {
            compose.onNodeWithContentDescription("Скопировать номер").performClick()
            compose.runOnIdle { assertEquals("+375 29 123 45 67", copied); assertEquals(null, opened) }
            compose.onNodeWithText("@coffee").performClick()
            compose.runOnIdle { assertEquals("https://instagram.com/coffee", opened) }
        }
    }

    @Test fun coffeeGroupsStaySeparateAndRoasterRowForwardsTheSlug() {
        var selected: String? = null
        render {
            ShopCoffeeDetailsSection(ShopCoffeeDetails(beans = listOf("Эфиопия"), equipment = listOf("V60"),
                roasters = listOf(ShopRoaster("roaster-slug", "Test roaster", null))), { selected = it })
        }.use {
            compose.onNodeWithText("Обжарщики").assertExists()
            compose.onNodeWithText("Кофе").assertExists()
            compose.onNodeWithText("Оборудование").assertExists()
            compose.onNodeWithText("Test roaster").performClick()
            compose.runOnIdle { assertEquals("roaster-slug", selected) }
        }
    }
}
