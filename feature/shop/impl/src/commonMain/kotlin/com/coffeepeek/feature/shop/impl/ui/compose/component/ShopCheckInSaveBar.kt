package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_submit
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInSaveBar(
    state: ShopCheckInFormState, photosPreparing: Boolean, onAction: (ShopCheckInFormAction) -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
        .navigationBarsPadding().padding(horizontal = CoffeePeekDimens.spacing4,
            vertical = CoffeePeekDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        ShopCheckInVisibilityField(state.input.visibility, state.isEditable(photosPreparing)) {
            onAction(ShopCheckInFormAction.VisibilityChanged(it))
        }
        CoffeePeekButton(
            text = stringResource(Res.string.shop_checkin_form_submit),
            enabled = state.isEditable(photosPreparing),
            onClick = {
                keyboard?.hide()
                focus.clearFocus(force = true)
                onAction(ShopCheckInFormAction.Submit)
            },
        )
    }
}

internal fun ShopCheckInFormState.isEditable(photosPreparing: Boolean): Boolean =
    !isLoading && !isSubmitting && !isDiscarding && !submitted && !deliveryUnconfirmed && !photosPreparing

@Preview @Composable private fun ShopCheckInSaveBarLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInSaveBar(ShopCheckInFormState(com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput(
        "shop", "", com.coffeepeek.feature.shop.domain.model.ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
        isLoading = false), false, {})
}

@Preview @Composable private fun ShopCheckInSaveBarDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInSaveBar(ShopCheckInFormState(com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput(
        "shop", "", com.coffeepeek.feature.shop.domain.model.ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
        isLoading = false), false, {})
}
