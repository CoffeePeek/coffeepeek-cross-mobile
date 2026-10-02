package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_draft_discard
import com.coffeepeek.feature.shop.impl.resources.shop_review_draft_restored
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReviewDraftNotice(onDiscard: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(CpIcons.NoteEdit, contentDescription = null)
        Text(stringResource(Res.string.shop_review_draft_restored),
            style = MaterialTheme.typography.labelMedium,
            modifier = androidx.compose.ui.Modifier.weight(1f))
        TextButton(onClick = onDiscard) {
            Text(stringResource(Res.string.shop_review_draft_discard))
        }
    }
}

@Preview @Composable private fun ShopReviewDraftNoticeLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewDraftNotice({})
}

@Preview @Composable private fun ShopReviewDraftNoticeDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewDraftNotice({})
}
