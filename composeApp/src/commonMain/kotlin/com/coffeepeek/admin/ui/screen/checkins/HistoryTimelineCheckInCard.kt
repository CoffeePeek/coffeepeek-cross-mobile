package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.admin.feature.checkin.api.CheckInTimelineContent
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.domain.model.CheckIn

/** History owns opening the saved entry; photos use the same viewer as the feed. */
@Composable
internal fun HistoryTimelineCheckInCard(
    entry: CheckIn,
    onClick: () -> Unit,
    onPhotoClick: (PhotoViewerImages, Int) -> Unit,
    supportingLabel: String? = null,
) {
    CheckInTimelineContent(
        checkIn = entry, isOwn = true,
        onShopClick = onClick.takeIf { !entry.isHome && entry.shopId.isNotBlank() },
        onPhotoClick = onPhotoClick,
        showHelpfulCount = entry.helpfulCount > 0,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        supportingContent = {
            supportingLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun HistoryTimelineCardPreview(darkTheme: Boolean) {
    CoffeePeekTheme(darkTheme = darkTheme) {
        HistoryTimelineCheckInCard(
            CheckIn("preview", "shop", "CoffeePeek", "Фильтр с ягодными нотами.", "2026-10-23T09:00:00Z"),
            onClick = {}, onPhotoClick = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun HistoryTimelineCardLightPreview() = HistoryTimelineCardPreview(darkTheme = false)

@Preview
@Composable
private fun HistoryTimelineCardDarkPreview() = HistoryTimelineCardPreview(darkTheme = true)
