package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryShopDetailSheet(entry: PersonalCheckIn, dateLabel: String, onDismiss: () -> Unit,
    onOpenShop: () -> Unit, onOpenPhoto: (Int) -> Unit, imageContent: HistoryImageContent?) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.history_close),
        maxSheetHeightFraction = 0.9f) {
        HistoryShopDetailContent(entry, dateLabel, onOpenShop, onOpenPhoto,
            Modifier.weight(1f), imageContent)
    }
}

@Composable private fun HistoryShopDetailContent(entry: PersonalCheckIn, dateLabel: String, onOpenShop: () -> Unit,
    onOpenPhoto: (Int) -> Unit, modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        HistoryCheckInCard(entry, dateLabel, onOpenPhoto, imageContent = imageContent)
        if (entry.shop?.slug?.isNotBlank() == true) TextButton(onOpenShop,
            Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(stringResource(Res.string.history_open_shop_action))
        }
    }
}

// Dialog placement is verified on the isolated host; IDE previews render the modal content.
@Composable private fun HistoryShopDetailPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryShopDetailContent(previewHistoryCheckIn(), "14 октября 2026", {}, {})
}
@Preview @Composable private fun HistoryShopDetailLightPreview() = HistoryShopDetailPreview(false)
@Preview @Composable private fun HistoryShopDetailDarkPreview() = HistoryShopDetailPreview(true)
