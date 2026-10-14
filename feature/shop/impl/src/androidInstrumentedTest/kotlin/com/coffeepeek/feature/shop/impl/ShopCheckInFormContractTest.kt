package com.coffeepeek.feature.shop.impl

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
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
        photosPreparing: Boolean = false,
        onAction: (ShopCheckInFormAction) -> Unit = {},
    ): ActivityScenario<ShopFormTestActivity> = ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
        scenario.onActivity { activity ->
            activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                    ShopCheckInCreateScreenContent(
                        state = value, shopName = "Тестовая кофейня", visitLabel = "9 октября 2026",
                        selectedVisitMillis = 1791504000000, nowMillis = 1791547200000,
                        onAction = onAction, onVisitDate = {}, onGallery = {}, onCamera = {},
                        photosPreparing = photosPreparing,
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
            compose.onNodeWithText("Сделать публичным").performScrollTo().performClick()
            compose.onNode(hasText("Чекин") and hasClickAction()).performScrollTo().performClick()
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
                SemanticsMatcher.expectValue(SemanticsProperties.Error, "Заметка не должна превышать 1000 символов"),
            )
        }
    }

    @Test fun productionLabelsAndEditableRatingCardsArePreserved() {
        val actions = mutableListOf<ShopCheckInFormAction>()
        render(initial.copy(input = initial.input.copy(text = "")), dark = true, onAction = actions::add).use {
            compose.onNodeWithText("Ваши оценки").assertIsDisplayed()
            compose.onNodeWithText("Нажмите на звёзды, чтобы изменить оценку").assertIsDisplayed()
            compose.onNodeWithContentDescription("Кофе: 5 из 5").performClick()
            compose.onNodeWithText("Дата посещения").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Расскажите о вашем визите…").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Фото").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Сделать публичным").performScrollTo().assertIsDisplayed()
            compose.runOnIdle { assertTrue(actions.contains(ShopCheckInFormAction.RatingChanged(ShopRating(4, 4, 5)))) }
        }
    }

    @Test fun unconfirmedDeliveryDisablesWriteAndRequiresExplicitHistoryAcknowledgement() {
        val actions = mutableListOf<ShopCheckInFormAction>()
        render(initial.copy(deliveryUnconfirmed = true), onAction = actions::add).use {
            compose.onNode(SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText, AnnotatedString(initial.input.text),
            )).performScrollTo().assertIsNotEnabled()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
            compose.onNode(hasText("Чекин") and hasClickAction()).performScrollTo().assertIsNotEnabled().performClick()
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
            compose.onNode(hasText("Чекин") and hasClickAction()).assertDoesNotExist()
            compose.onNodeWithText("К ленте").assertDoesNotExist()
            compose.onNodeWithText("Мои чекины").assertDoesNotExist()
        }
    }

    @Test fun preparingPhotosDisablesSubmitWithoutEmittingWrite() {
        val actions = mutableListOf<ShopCheckInFormAction>()
        render(initial, photosPreparing = true, onAction = actions::add).use {
            compose.onNode(hasText("Чекин") and hasClickAction()).performScrollTo().assertIsNotEnabled().performClick()
            compose.runOnIdle { assertTrue(actions.isEmpty()) }
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
                compose.onNode(hasText("Чекин") and hasClickAction()).assertDoesNotExist()
                compose.onNodeWithText("К ленте").performClick()
                compose.onNodeWithText("Мои чекины").performClick()
                compose.runOnIdle {
                    assertEquals(listOf(ShopCheckInFormAction.GoToFeed, ShopCheckInFormAction.OpenHistory), actions)
                }
            }
        }
    }
}
