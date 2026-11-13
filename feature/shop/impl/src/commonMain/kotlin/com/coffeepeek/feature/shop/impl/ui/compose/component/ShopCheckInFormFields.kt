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
import org.jetbrains.compose.ui.tooling.preview.Preview

/** One scrollable sequence: place and date, then the visit details. */
@Composable
internal fun ShopCheckInFormFields(
    state: ShopCheckInFormState, shopName: String, shopAddress: String?, visitLabel: String,
    selectedVisitMillis: Long?, nowMillis: Long, editable: Boolean, photosPreparing: Boolean,
    onAction: (ShopCheckInFormAction) -> Unit, onChangeShop: () -> Unit,
    onVisitDate: (Long) -> Unit, onGallery: (Int) -> Unit, onCamera: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ShopCheckInFormHeader(shopName, shopAddress, editable, onChangeShop)
        ShopCheckInDateField(visitLabel, selectedVisitMillis, nowMillis, editable, onVisitDate)
        ShopCheckInDrinkInput(state, editable, onAction)
        ShopCheckInRatings(state.input.rating, editable) {
            onAction(ShopCheckInFormAction.RatingChanged(it))
        }
        ShopCheckInNoteField(state.input.text, editable,
            state.validationError == ShopCheckInValidationError.InvalidNote) {
            onAction(ShopCheckInFormAction.TextChanged(it))
        }
        ShopCheckInPhotoAttachments(
            photos = state.input.photos.map { it.bytes }, isLoading = photosPreparing, enabled = editable,
            onMove = { from, to -> onAction(ShopCheckInFormAction.MovePhoto(from, to)) },
            onRemove = { onAction(ShopCheckInFormAction.RemovePhoto(it)) },
            onGallery = onGallery, onCamera = onCamera)
    }
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
        { onAction(ShopCheckInFormAction.CustomDrinkChanged(it)) },
    )
}

@Composable
private fun ShopCheckInFieldsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShopCheckInFormFields(
        ShopCheckInFormState(ShopCheckInCreateInput("coffee-room", "", ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
            isLoading = false), "The Coffee Room", "ул. Петра Мстиславца, 20", "Сегодня",
        null, 1791547200000, true, false, {}, {}, {}, {}, {})
}

@Preview @Composable private fun ShopCheckInFieldsLightPreview() = ShopCheckInFieldsPreview(false)
@Preview @Composable private fun ShopCheckInFieldsDarkPreview() = ShopCheckInFieldsPreview(true)
