package com.coffeepeek.admin.feature.checkin.ui

import com.coffeepeek.admin.ui.component.preparationMetricLabels

import com.coffeepeek.admin.ui.component.PreparationMetricLabels

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CheckInHeader
import com.coffeepeek.admin.ui.component.CheckInHeading
import com.coffeepeek.admin.ui.component.HelpfulButton
import com.coffeepeek.admin.ui.component.ReviewPhotoStrip
import com.coffeepeek.admin.ui.component.PhotoViewerImages
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
    onPhotoClick: (PhotoViewerImages, Int) -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    onHelpful: (() -> Unit)? = null,
    showHelpfulCount: Boolean = true,
    displayTimestamp: String = checkIn.createdAt,
    actions: @Composable () -> Unit = {},
    supportingContent: @Composable () -> Unit = {},
    footerActions: (@Composable () -> Unit)? = null,
) {
    val localizedLegacyReviewContentOwnershipKindShop = stringResource(Res.string.legacy_review_content_ownership_kind_shop)

    val drinkName = savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        CheckInHeader(checkIn, isOwn, onAuthorClick = onAuthorClick, displayTimestamp = displayTimestamp, actions = actions)
        if (checkIn.isHome) {
            checkIn.homePreparation?.let { recipe ->
                val details = recipe.checkInDetails(preparationMetricLabels())
                // A shown bag is the title and carries the recipe line itself.
                recipe.shelfBagCardUrl?.let { card ->
                    CheckInShelfBag(card, recipe.beanName, recipe.roasterName, details,
                        recipe.coffeeProductSlug?.let { slug -> { Navigator.navigate(Navigator.Screen.CoffeeDetail(slug)) } })
                } ?: run {
                    recipe.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else CheckInHeading(
            shopName = checkIn.shopName.ifBlank { localizedLegacyReviewContentOwnershipKindShop },
            rating = checkIn.rating,
            onShopClick = onShopClick,
        )
        if (checkIn.note.isNotBlank()) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
        ReviewPhotoStrip(
            photoUrls = checkIn.photoDetailUrls ?: checkIn.photoUrls,
            thumbnailUrls = checkIn.photoThumbnailUrls,
            originalUrls = checkIn.photoUrls,
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

private fun FeedHomePreparation.checkInDetails(labels: PreparationMetricLabels): String = listOfNotNull(
    brewMethodCode?.methodLabel(labels.otherMethod),
    doseGrams?.let { labels.grams.replace("%1\$s", it.toString()) },
    brewDurationSeconds?.let { labels.seconds.replace("%1\$s", it.toString()) },
    tasteScore?.let { "$it/5" },
).joinToString(" · ")
