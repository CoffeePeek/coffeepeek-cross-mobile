package com.coffeepeek.feature.shop.impl

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInCreateScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShopFormTestActivity : ComponentActivity()

/** Stateless UI only: no production account, DI, network, photo picker or database. */
class ShopCheckInFormContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val initial = ShopCheckInFormState(
        input = ShopCheckInCreateInput("test-shop", "Заметка", ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
        isLoading = false,
    )

    private fun render(
        value: ShopCheckInFormState,
        dark: Boolean = false,
        onAction: (ShopCheckInFormAction) -> Unit = {},
    ): ActivityScenario<ShopFormTestActivity> = ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
        scenario.onActivity { activity ->
            activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                    ShopCheckInCreateScreenContent(
                        state = value, shopName = "Тестовая кофейня", visitLabel = "9 октября 2026",
                        selectedVisitMillis = 1791504000000, nowMillis = 1791547200000,
                        onAction = onAction, onVisitDate = {}, onGallery = {}, onCamera = {},
                    )
                }
            }
        }
    }

    @Test fun editableFormEmitsIntentsAndCatalogRetry() {
        val actions = mutableListOf<ShopCheckInFormAction>()
        render(initial.copy(drinksFailed = true), onAction = actions::add).use {
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Новая заметка")
            compose.onNodeWithText("Повторить").performScrollTo().performClick()
            compose.onNodeWithText("Поделиться в ленте").performScrollTo().performClick()
            compose.onNodeWithText("Оставить чекин").performScrollTo().performClick()
            compose.runOnIdle {
                assertTrue(actions.contains(ShopCheckInFormAction.TextChanged("Новая заметка")))
                assertTrue(actions.contains(ShopCheckInFormAction.RetryDrinks))
                assertTrue(actions.contains(ShopCheckInFormAction.VisibilityChanged(ShopCheckInVisibility.Public)))
                assertEquals(ShopCheckInFormAction.Submit, actions.last())
            }
        }
    }

    @Test fun noteErrorExposesLocalizedAccessibilityMessage() {
        render(initial.copy(validationError = ShopCheckInValidationError.InvalidNote), dark = true).use {
            compose.onNode(hasSetTextAction()).performScrollTo().assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Error, "Введите заметку: от 1 до 1000 символов"),
            )
        }
    }

    @Test fun unconfirmedDeliveryDisablesWriteAndRequiresExplicitHistoryAcknowledgement() {
        val actions = mutableListOf<ShopCheckInFormAction>()
        render(initial.copy(deliveryUnconfirmed = true), onAction = actions::add).use {
            compose.onNode(hasSetTextAction()).performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("Оставить чекин").performScrollTo().assertIsNotEnabled().performClick()
            compose.onNodeWithText("Мои чекины").performScrollTo().performClick()
            compose.onNodeWithText("Проверил историю — чекин не создан").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(listOf(ShopCheckInFormAction.OpenHistory, ShopCheckInFormAction.HistoryChecked), actions)
            }
        }
    }

    @Test fun pendingWriteHasNoSubmitOrDestinationControls() {
        render(initial.copy(isSubmitting = true)).use {
            compose.onNodeWithText("Отправляем чекин…").assertIsDisplayed()
            compose.onNodeWithText("Оставить чекин").assertDoesNotExist()
            compose.onNodeWithText("К ленте").assertDoesNotExist()
            compose.onNodeWithText("Мои чекины").assertDoesNotExist()
        }
    }

    @Test fun privateAndPublicConfirmationPreserveCallerOwnedDestinations() {
        for (visibility in ShopCheckInVisibility.entries) {
            val actions = mutableListOf<ShopCheckInFormAction>()
            render(initial.copy(input = initial.input.copy(visibility = visibility), submitted = true),
                dark = visibility == ShopCheckInVisibility.Public, onAction = actions::add).use {
                compose.onNodeWithText("Чекин создан!").assertIsDisplayed()
                compose.onNodeWithText(if (visibility == ShopCheckInVisibility.Public)
                    "Спасибо, что делитесь кофейными моментами! Чекин появится в ленте после проверки. Сейчас он доступен в ваших чекинах."
                    else "Кофейный момент сохранён в ваших чекинах и виден только вам.").assertIsDisplayed()
                compose.onNodeWithText("Оставить чекин").assertDoesNotExist()
                compose.onNodeWithText("К ленте").performClick()
                compose.onNodeWithText("Мои чекины").performClick()
                compose.runOnIdle {
                    assertEquals(listOf(ShopCheckInFormAction.GoToFeed, ShopCheckInFormAction.OpenHistory), actions)
                }
            }
        }
    }
}
