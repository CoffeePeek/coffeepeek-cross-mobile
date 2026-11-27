package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInEditorStep
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Both steps share one input/draft; changing the visible fields never resets it. */
@Composable
internal fun ShopCheckInFormFields(
    state: ShopCheckInFormState, shopName: String, shopAddress: String?, visitLabel: String,
    selectedVisitMillis: Long?, nowMillis: Long, editable: Boolean, photosPreparing: Boolean,
    onAction: (ShopCheckInFormAction) -> Unit,
    onVisitDate: (Long) -> Unit, onGallery: (Int) -> Unit, onCamera: () -> Unit,
    step: ShopCheckInEditorStep = ShopCheckInEditorStep.Ratings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        when (step) {
            ShopCheckInEditorStep.Ratings -> ShopCheckInExperienceFields(state, editable, photosPreparing, onAction, onGallery, onCamera)
            ShopCheckInEditorStep.Details -> {
                ShopCheckInFormHeader(shopName, shopAddress)
                ShopCheckInDateField(visitLabel, selectedVisitMillis, nowMillis, editable, onVisitDate)
                ShopCheckInDrinkInput(state, editable, onAction)
                ShopCheckInVisibilityField(state.input.visibility, editable) {
                    onAction(ShopCheckInFormAction.VisibilityChanged(it))
                }
            }
        }
    }
}

@Composable
private fun ShopCheckInExperienceFields(state: ShopCheckInFormState, editable: Boolean, photosPreparing: Boolean,
    onAction: (ShopCheckInFormAction) -> Unit, onGallery: (Int) -> Unit, onCamera: () -> Unit) {
    ShopCheckInRatings(state.input.rating, editable) { onAction(ShopCheckInFormAction.RatingChanged(it)) }
    ShopCheckInNoteField(state.input.text, editable, state.validationError == ShopCheckInValidationError.InvalidNote) {
        onAction(ShopCheckInFormAction.TextChanged(it))
    }
    ShopCheckInPhotoAttachments(state.input.photos.map { it.bytes }, photosPreparing, editable,
        onMove = { from, to -> onAction(ShopCheckInFormAction.MovePhoto(from, to)) },
        onRemove = { onAction(ShopCheckInFormAction.RemovePhoto(it)) }, onGallery = onGallery, onCamera = onCamera)
}

@Composable
private fun ShopCheckInDrinkInput(
    state: ShopCheckInFormState, editable: Boolean, onAction: (ShopCheckInFormAction) -> Unit,
) {
    ShopCheckInDrinkField(
        state.drinks, state.input.drinkSlug, state.input.customDrinkName,
        state.drinksLoading, state.drinksFailed, editable,
        state.validationError == ShopCheckInValidationError.InvalidDrink,
        { onAction(ShopCheckInFormAction.RetryDrinks) },
        { onAction(ShopCheckInFormAction.DrinkChanged(it)) },
    )
}

@Composable
private fun ShopCheckInFieldsPreview(dark: Boolean, step: ShopCheckInEditorStep = ShopCheckInEditorStep.Ratings) = CoffeePeekTheme(darkTheme = dark) {
    ShopCheckInFormFields(
        ShopCheckInFormState(ShopCheckInCreateInput("coffee-room", "", ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
            isLoading = false), "The Coffee Room", "ул. Петра Мстиславца, 20", "Сегодня",
        null, 1791547200000, true, false, {}, {}, {}, {}, step)
}

@Preview @Composable private fun ShopCheckInFieldsLightPreview() = ShopCheckInFieldsPreview(false)
@Preview @Composable private fun ShopCheckInFieldsDarkPreview() = ShopCheckInFieldsPreview(true)

@Preview @Composable private fun ShopCheckInDetailsLightPreview() = ShopCheckInFieldsPreview(false, ShopCheckInEditorStep.Details)
@Preview @Composable private fun ShopCheckInDetailsDarkPreview() = ShopCheckInFieldsPreview(true, ShopCheckInEditorStep.Details)
