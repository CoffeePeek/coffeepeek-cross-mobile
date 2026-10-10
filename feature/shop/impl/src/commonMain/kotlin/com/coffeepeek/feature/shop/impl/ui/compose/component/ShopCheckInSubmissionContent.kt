package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.core.designsystem.component.AppButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_sending
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_success
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_success_public
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_success_private
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_feed
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_history
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_cleanup_error
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInSubmissionContent(
    submittedVisibility: ShopCheckInVisibility?, cleanupFailed: Boolean, onFeed: () -> Unit, onHistory: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
        if (submittedVisibility == null) {
            CircularProgressIndicator()
            Text(stringResource(Res.string.shop_checkin_form_sending), style = MaterialTheme.typography.headlineSmall)
        } else {
            Text(stringResource(Res.string.shop_checkin_form_success), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(if (submittedVisibility == ShopCheckInVisibility.Public)
                Res.string.shop_checkin_form_success_public else Res.string.shop_checkin_form_success_private))
            if (cleanupFailed) Text(stringResource(Res.string.shop_checkin_form_cleanup_error),
                color = MaterialTheme.colorScheme.error)
            AppButton(stringResource(Res.string.shop_checkin_form_feed), onFeed)
            TextButton(onClick = onHistory) { Text(stringResource(Res.string.shop_checkin_form_history)) }
        }
    }
}

@Preview @Composable private fun ShopCheckInSubmissionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInSubmissionContent(ShopCheckInVisibility.Private, false, {}, {})
}
@Preview @Composable private fun ShopCheckInSubmissionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInSubmissionContent(ShopCheckInVisibility.Public, false, {}, {})
}
@Preview @Composable private fun ShopCheckInSendingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInSubmissionContent(null, false, {}, {})
}
@Preview @Composable private fun ShopCheckInSendingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInSubmissionContent(null, false, {}, {})
}
