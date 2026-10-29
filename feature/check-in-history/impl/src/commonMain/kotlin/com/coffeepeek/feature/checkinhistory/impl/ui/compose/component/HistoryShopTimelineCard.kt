package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineContent
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineHeader
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineFooter
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.displayName
import com.coffeepeek.feature.checkinhistory.impl.ui.data.timelinePhotos
import com.coffeepeek.feature.checkinhistory.impl.ui.data.completeTimelineRating
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryShopTimelineCard(entry: PersonalCheckIn, dateLabel: String, onClick: () -> Unit,
    onPhoto: (Int) -> Unit, image: HistoryImageContent?) {
    val name = entry.username.ifBlank { stringResource(Res.string.history_you) }
    val photos = entry.timelinePhotos()
    CoffeePeekTimelineContent(entry.note, Modifier.clickable(role = Role.Button, onClick = onClick),
        header = {
            CoffeePeekTimelineHeader(name, dateLabel, avatar = { modifier ->
                entry.authorAvatarUrl?.let { HistoryPhoto(it, modifier, imageContent = image) }
            })
        }, heading = { HistoryTimelineShopHeading(entry, onClick) },
        photos = { HistoryTimelinePhotosContent(photos, onPhoto, image) },
        footer = {
            CoffeePeekTimelineFooter(entry.helpfulCount.takeIf { it > 0 }, entry.isHelpfulByCurrentUser,
                stringResource(Res.string.history_helpful_count, entry.helpfulCount), entry.drink.displayName(Locale.current.language))
        })
}

@Composable
private fun HistoryTimelineShopHeading(entry: PersonalCheckIn, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.shopName.ifBlank { stringResource(Res.string.history_shop) },
            Modifier.then(if (!entry.shop?.slug.isNullOrBlank()) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        entry.rating.completeTimelineRating()?.let { HistoryCheckInRating(it) }
    }
}

@Composable private fun ShopTimelinePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryShopTimelineCard(previewHistoryCheckIn(), "14 октября 2026", {}, {}, null)
}
@Preview @Composable private fun ShopTimelineLightPreview() = ShopTimelinePreview(false)
@Preview @Composable private fun ShopTimelineDarkPreview() = ShopTimelinePreview(true)
