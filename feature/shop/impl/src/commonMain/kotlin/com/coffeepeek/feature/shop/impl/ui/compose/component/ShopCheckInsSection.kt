package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInsSection(
    checkIns: List<ShopCheckIn>,
    onOpenPhoto: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (checkIns.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        Text(stringResource(Res.string.shop_checkins_title), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            items(checkIns, key = ShopCheckIn::id) { checkIn ->
                ShopCheckInCard(checkIn, onOpenPhoto, Modifier.width(300.dp))
            }
        }
    }
}

@Preview @Composable private fun ShopCheckInsSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInsSection(listOf(previewCheckIn()), onOpenPhoto = { _, _ -> })
}

@Preview @Composable private fun ShopCheckInsSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInsSection(listOf(previewCheckIn()), onOpenPhoto = { _, _ -> })
}
