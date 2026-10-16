package com.coffeepeek.admin.feature.checkin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CheckInHeader
import com.coffeepeek.admin.ui.component.CheckInHeading
import com.coffeepeek.admin.ui.component.HelpfulButton
import com.coffeepeek.admin.ui.component.ReviewPhotoStrip
import com.coffeepeek.admin.ui.component.SavedDrinkLabel
import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.savedDrinkName

/** Shared presentation for timeline entries; each screen supplies its supported actions. */
@Composable
internal fun CheckInTimelineContentImpl(
    checkIn: CheckIn,
    isOwn: Boolean,
    onShopClick: (() -> Unit)?,
    onPhotoClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    onHelpful: (() -> Unit)? = null,
    showHelpfulCount: Boolean = true,
    actions: @Composable () -> Unit = {},
    supportingContent: @Composable () -> Unit = {},
    footerActions: (@Composable () -> Unit)? = null,
) {
    val drinkName = savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        CheckInHeader(checkIn, isOwn, onAuthorClick = onAuthorClick, actions = actions)
        if (checkIn.isHome) {
            checkIn.homePreparation?.let { recipe ->
                recipe.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                val details = recipe.checkInDetails()
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else CheckInHeading(
            shopName = checkIn.shopName.ifBlank { "Кофейня" },
            rating = checkIn.rating,
            onShopClick = onShopClick,
        )
        if (checkIn.note.isNotBlank()) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
        ReviewPhotoStrip(
            photoUrls = checkIn.photoUrls,
            thumbnailUrls = checkIn.photoThumbnailUrls,
            singlePhotoAspectRatio = 2f,
            onPhotoClick = onPhotoClick,
        )
        supportingContent()
        if (showHelpfulCount || drinkName != null || footerActions != null) Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            if (showHelpfulCount) HelpfulButton(checkIn.helpfulCount, checkIn.isHelpfulByCurrentUser, onHelpful)
            Spacer(Modifier.weight(1f))
            footerActions?.invoke()
            drinkName?.let { SavedDrinkLabel(it) }
        }
    }
}

private fun FeedHomePreparation.checkInDetails(): String = listOfNotNull(
    brewMethodCode?.methodLabel(),
    doseGrams?.let { "$it г" },
    brewDurationSeconds?.let { "$it с" },
    tasteScore?.let { "$it/5" },
).joinToString(" · ")
