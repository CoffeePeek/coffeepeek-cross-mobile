package com.coffeepeek.feature.shop.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInDrinkField
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPhotoAttachments
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPhotoSourceSheetContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fake, local UI only; no photo picker, camera, backend or production account. */
class ShopSelectionControlsTest {
    @get:Rule val compose = createEmptyComposeRule()
    // This fixture tests selection, not locale switching: labels stay deterministic on any runner.
    private val options = listOf(ShopConsumedDrinkOption("espresso", "Эспрессо", "Эспрессо"),
        ShopConsumedDrinkOption("cappuccino", "Капучино", "Капучино"),
        ShopConsumedDrinkOption("other", "Другое", "Другое"))

    private fun render(dark: Boolean = false, content: @Composable () -> Unit): ActivityScenario<ShopFormTestActivity> =
        ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(16.dp)) { content() } }
                }
            } }
        }

    @Test fun noneIsInitiallySelectedWithoutMutatingDraftAndCanBeRestoredInBothThemes() {
        listOf(false, true).forEach { dark ->
            val choices = mutableListOf<String?>()
            render(dark) {
                var slug by remember { mutableStateOf<String?>(null) }
                ShopCheckInDrinkField(options, slug, null, false, false, true, false, {},
                    { choices += it; slug = it }, {})
            }.use {
                compose.runOnIdle { assertTrue(choices.isEmpty()) }
                compose.onNodeWithText("Не выбран").performClick()
                compose.onNode(hasText("Не выбран") and hasAnyAncestor(isPopup())).assertIsSelected()
                compose.onAllNodes(isSelected()).assertCountEquals(1)
                compose.onNode(hasText("Эспрессо") and hasAnyAncestor(isPopup())).performClick()
                compose.onNodeWithText("Эспрессо").performClick()
                compose.onNode(hasText("Эспрессо") and hasAnyAncestor(isPopup())).assertIsSelected()
                compose.onNode(hasText("Не выбран") and hasAnyAncestor(isPopup())).performClick()
                compose.runOnIdle { assertEquals(listOf("espresso", null), choices) }
            }
        }
    }

    @Test fun choosingAlreadySelectedCustomDrinkDoesNotResetItsName() {
        val choices = mutableListOf<String?>()
        render {
            ShopCheckInDrinkField(options, "other", "Авторский капучино", false, false, true, false, {}, choices::add, {})
        }.use {
            compose.onAllNodes(hasText("Авторский капучино"))[0].performClick()
            compose.onNode(hasText("Другое") and hasAnyAncestor(isPopup())).assertIsSelected().performClick()
            compose.runOnIdle { assertTrue(choices.isEmpty()) }
        }
    }

    @Test fun missingCatalogOptionRemainsSelectedInsteadOfPretendingNoneWasSaved() {
        render(dark = true) {
            ShopCheckInDrinkField(options, "saved-slug", null, false, false, true, false, {}, {}, {})
        }.use {
            compose.onNodeWithText("saved-slug").performClick()
            compose.onNode(hasText("saved-slug") and hasAnyAncestor(isPopup())).assertIsSelected()
            compose.onAllNodes(isSelected()).assertCountEquals(1)
        }
    }

    @Test fun loadingAndLockedDrinkFieldsCannotOpenMenu() {
        render {
            ShopCheckInDrinkField(options, null, null, true, false, false, false, {}, {}, {})
        }.use {
            compose.onNodeWithText("Не выбран").assertIsNotEnabled().performClick()
            compose.onAllNodes(isPopup()).assertCountEquals(0)
        }
    }

    @Test fun photoSourceRowsAndCancelForwardOnlyTheirOwnCallback() {
        listOf(false, true).forEach { dark ->
            val actions = mutableListOf<String>()
            render(dark) { ShopPhotoSourceSheetContent({ actions += "cancel" }, { actions += "gallery" }, { actions += "camera" }) }
                .use {
                    compose.onNodeWithText("Выбрать из галереи").performClick()
                    compose.onNodeWithText("Сделать фото").performClick()
                    compose.onNodeWithText("Отмена").performClick()
                    compose.runOnIdle { assertEquals(listOf("gallery", "camera", "cancel"), actions) }
                }
        }
    }

    @Test fun realPhotoSourceModalPreservesRemainingCapacityAndClosesBeforePickerCallback() {
        val capacities = mutableListOf<Int>()
        var cameras = 0
        render(dark = true) {
            ShopPhotoAttachments(emptyList(), false, true, "Фото", "До 5 фото", 5, {}, capacities::add, { cameras++ })
        }.use {
            compose.onNodeWithContentDescription("Добавить фото").performClick()
            compose.onNodeWithText("Выбрать из галереи").performClick()
            compose.onNodeWithText("Сделать фото").assertDoesNotExist()
            compose.onNodeWithContentDescription("Добавить фото").performClick()
            compose.onNodeWithText("Сделать фото").performClick()
            compose.runOnIdle { assertEquals(listOf(5), capacities); assertEquals(1, cameras) }
        }
    }
}
