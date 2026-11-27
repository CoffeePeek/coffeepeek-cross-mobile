package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCreationSuccess
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_cleanup_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_dismiss
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_feed
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_sending
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_success_private
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_success_public
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_success_published
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInSubmissionContent(
    visibility: ShopCheckInVisibility, cleanupFailed: Boolean, onFeed: () -> Unit, onClose: () -> Unit,
) {
    CoffeePeekCreationSuccess(
        title = stringResource(Res.string.shop_checkin_form_success_published),
        message = stringResource(if (visibility == ShopCheckInVisibility.Public)
            Res.string.shop_checkin_form_success_public else Res.string.shop_checkin_form_success_private),
        buttonLabel = stringResource(Res.string.shop_checkin_form_feed),
        closeLabel = stringResource(Res.string.shop_checkin_form_dismiss),
        onClose = onClose,
        onDone = onFeed,
    ) {
        if (cleanupFailed) Text(
            stringResource(Res.string.shop_checkin_form_cleanup_error),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ShopCheckInSendingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        CoffeePeekLoader(size = 40.dp, color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
        Text(stringResource(Res.string.shop_checkin_form_sending),
            style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
}

@Preview @Composable private fun ShopCheckInSubmissionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInSubmissionContent(ShopCheckInVisibility.Private, false, {}, {})
}
@Preview @Composable private fun ShopCheckInSubmissionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInSubmissionContent(ShopCheckInVisibility.Public, true, {}, {})
}
@Preview @Composable private fun ShopCheckInSendingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInSendingContent()
}
@Preview @Composable private fun ShopCheckInSendingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInSendingContent()
}
