package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.*

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeIcon

import com.coffeepeek.domain.model.savedDrinkName
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.formatOneDecimal
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.coffeepeek_legacy_choose_rating
import coffeepeek.composeapp.generated.resources.coffeepeek_legacy_rating_description
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import coffeepeek.composeapp.generated.resources.checkin_rating_atmosphere
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import coffeepeek.composeapp.generated.resources.checkin_rating_service
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.ExperimentalTime

@Composable
fun ReviewFormField(
    label: String,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.semantics { error?.let { error(it) } },
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
        error?.let { ReviewFieldError(it) }
    }
}

@Composable
internal fun ReviewFieldError(message: String) {
    val color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
        lerp(MaterialTheme.colorScheme.error, Color.White, 0.3f) else MaterialTheme.colorScheme.error
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top) {
        Icon(CpIcons.Error, null, Modifier.size(18.dp), tint = color)
        Text(message, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
fun ReviewTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    singleLine: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    val shape = if (singleLine) {
        RoundedCornerShape(percent = 50)
    } else {
        RoundedCornerShape(CpDimens.buttonRadius)
    }
    val fieldValue = rememberSyncedTextFieldValue(value)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = fieldValue.value,
        onValueChange = { updated ->
            val limited = updated.limitTextLength(maxLength)
            fieldValue.updateFromInput(limited, onValueChange)
        },
        keyboardOptions = platformTextInputOptions(KeyboardOptions(
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        )),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }),
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .then(if (singleLine) Modifier.height(CpDimens.buttonHeight) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = if (singleLine) Modifier.fillMaxSize() else Modifier,
                contentAlignment = Alignment.CenterStart,
            ) {
                if (fieldValue.value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
fun ReviewRatingCards(
    coffeeRating: Int,
    serviceRating: Int,
    placeRating: Int,
    onCoffeeRatingChange: (Int) -> Unit,
    onServiceRatingChange: (Int) -> Unit,
    onPlaceRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localizedLegacyReviewContentCommunityRatingsTitle = stringResource(Res.string.legacy_review_content_community_ratings_title)
    val localizedLegacyReviewContentCommunityRatingsHint = stringResource(Res.string.legacy_review_content_community_ratings_hint)
    val localizedCheckinRatingCoffee = stringResource(Res.string.checkin_rating_coffee)
    val localizedCheckinRatingService = stringResource(Res.string.checkin_rating_service)
    val localizedLegacyReviewContentCommunityRatingAtmosphere = stringResource(Res.string.legacy_review_content_community_rating_atmosphere)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = localizedLegacyReviewContentCommunityRatingsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = localizedLegacyReviewContentCommunityRatingsHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EditableRatingCard(
            image = Res.drawable.checkin_rating_coffee,
            label = localizedCheckinRatingCoffee,
            rating = coffeeRating,
            onRatingChange = onCoffeeRatingChange,
        )
        EditableRatingCard(
            image = Res.drawable.checkin_rating_service,
            label = localizedCheckinRatingService,
            rating = serviceRating,
            onRatingChange = onServiceRatingChange,
        )
        EditableRatingCard(
            image = Res.drawable.checkin_rating_atmosphere,
            label = localizedLegacyReviewContentCommunityRatingAtmosphere,
            rating = placeRating,
            onRatingChange = onPlaceRatingChange,
        )
    }
}

@Composable
internal fun EditableRatingCard(
    image: DrawableResource,
    label: String,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(CpDimens.radiusMd))
            .padding(horizontal = CpDimens.spacing2, vertical = CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(CpDimens.radiusSm)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            CoffeePeekRating(rating.toDouble(), size = CoffeePeekRatingSize.Large,
                onRatingChange = onRatingChange,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                starDescription = { stringResource(Res.string.coffeepeek_legacy_choose_rating, label, it) })
        }
    }
}

@Composable
fun ReviewRatingSummary(rating: ReviewRating, modifier: Modifier = Modifier) {
    val localizedCheckinRatingCoffee = stringResource(Res.string.checkin_rating_coffee)
    val localizedCheckinRatingService = stringResource(Res.string.checkin_rating_service)
    val localizedLegacyReviewContentCommunityRatingAtmosphere = stringResource(Res.string.legacy_review_content_community_rating_atmosphere)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        RatingValue(localizedCheckinRatingCoffee, rating.coffee)
        RatingValue(localizedCheckinRatingService, rating.service)
        RatingValue(localizedLegacyReviewContentCommunityRatingAtmosphere, rating.place)
    }
}

/** Shared check-in presentation; legacy read models remain until the API migration. */
@Composable
internal fun CheckInCard(
    modifier: Modifier,
    blurContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.then(if (blurContent) Modifier.blur(5.dp) else Modifier).padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            content = content,
        )
    }
}

@Composable
fun ReviewDisplayCard(
    review: Review,
    modifier: Modifier = Modifier,
    onPhotoClick: ((PhotoViewerImages, Int) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onHelpfulClick: (() -> Unit)? = null,
    showHelpfulButton: Boolean = false,
    fullVersion: Boolean = false,
    /** Side-by-side rows: pad short comments too, so neighbouring cards end up about the same height. */
    equalizeHeight: Boolean = false,
    blurContent: Boolean = false,
    authorPhotoUrl: String? = null,
    dateLabel: String? = null,
    footer: (@Composable () -> Unit)? = null,
    showDrinkBadge: Boolean = true,
    shopName: String? = null,
) {
    CheckInCard(modifier, blurContent) {
        ReviewHeader(review = review, onEditClick = onEditClick, onReportClick = onReportClick, authorPhotoUrl = authorPhotoUrl, dateLabel = dateLabel)

        val heading = shopName?.takeIf(String::isNotBlank) ?: review.header
        CheckInHeading(heading.takeIf(String::isNotBlank), review.rating)
        if (showDrinkBadge) savedDrinkName(review.drinkNameRu, review.drinkNameEn, review.customDrinkName, Locale.current.language)?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SavedDrinkLabel(it) }
        }
        if (!shopName.isNullOrBlank() && review.header.isNotBlank() && review.header != heading) Text(
            review.header, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
        )
        if (review.comment.isNotBlank()) {
            if (fullVersion) Text(review.comment, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            else ReviewQuote(review.id, review.comment, padToCollapsedLines = equalizeHeight)
        }

        ReviewPhotoStrip(
            photoUrls = review.photoUrls,
            onPhotoClick = onPhotoClick,
        )

        if (showHelpfulButton) {
            HelpfulButton(
                helpfulCount = review.helpfulCount,
                isHelpful = review.isHelpfulByCurrentUser,
                onClick = onHelpfulClick,
            )
        }
        footer?.invoke()
    }
}

@Composable
fun CheckInDisplayCard(
    checkIn: CheckIn,
    modifier: Modifier = Modifier,
    showShopName: Boolean = true,
    onClick: (() -> Unit)? = null,
    onPhotoClick: ((PhotoViewerImages, Int) -> Unit)? = null,
    onHelpfulClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    isOwn: Boolean = true,
    fullVersion: Boolean = false,
    blurContent: Boolean = false,
    onAuthorClick: (() -> Unit)? = null,
) {
    val localizedLegacyReviewContentOwnershipKindShop = stringResource(Res.string.legacy_review_content_ownership_kind_shop)

    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null && checkIn.shopId.isNotBlank()) Modifier.clickable(onClick = onClick) else Modifier)

    CheckInCard(cardModifier, blurContent) {
        CheckInHeader(checkIn = checkIn, isOwn = isOwn, onAuthorClick = onAuthorClick) {
            CheckInActions(onEditClick = null, onReportClick = onReportClick, author = checkIn.blockableAuthor(stringResource(Res.string.legacy_author_fallback)).takeUnless { isOwn })
        }
        CheckInHeading(checkIn.shopName.ifBlank { localizedLegacyReviewContentOwnershipKindShop }.takeIf { showShopName }, checkIn.rating)
        val drinkName = savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)
        if (checkIn.note.isNotBlank()) {
            if (fullVersion) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            else ReviewQuote(checkIn.id, checkIn.note, padToCollapsedLines = false)
        }
        ReviewPhotoStrip(
            photoUrls = checkIn.photoDetailUrls ?: checkIn.photoUrls,
            thumbnailUrls = checkIn.photoThumbnailUrls,
            originalUrls = checkIn.photoUrls,
            onPhotoClick = onPhotoClick,
        )
        if (checkIn.visibility == CheckInVisibility.Public || checkIn.helpfulCount > 0 || drinkName != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (checkIn.visibility == CheckInVisibility.Public || checkIn.helpfulCount > 0) {
                    HelpfulButton(checkIn.helpfulCount, checkIn.isHelpfulByCurrentUser, onClick = onHelpfulClick)
                }
                Spacer(Modifier.weight(1f))
                drinkName?.let { SavedDrinkLabel(it) }
            }
        }
    }
}

@Composable
internal fun CheckInHeader(
    checkIn: CheckIn,
    isOwn: Boolean,
    onAuthorClick: (() -> Unit)? = null,
    displayTimestamp: String = checkIn.createdAt,
    actions: @Composable () -> Unit,
) {
    val localizedLegacyReviewContentCommunityYou = stringResource(Res.string.legacy_review_content_community_you)
    val localizedLegacyReviewContentCommunityUser = stringResource(Res.string.legacy_review_content_community_user)

    val date = displayTimestamp
    val username = checkIn.username.ifBlank { if (isOwn) localizedLegacyReviewContentCommunityYou else localizedLegacyReviewContentCommunityUser }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(CpDimens.radiusMd))
                .then(if (onAuthorClick != null) Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(Res.string.legacy_profile_open_named, username),
                    onClick = onAuthorClick,
                ) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReviewAvatar(username = username, photoUrl = checkIn.authorAvatarUrl)
            Column(
                modifier = Modifier.weight(1f).padding(start = CpDimens.spacing3),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (date.isNotBlank()) Text(
                    text = rememberReviewDisplayDate(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

@Composable
internal fun CheckInHeading(
    shopName: String?,
    rating: ReviewRating?,
    onShopClick: (() -> Unit)? = null,
) {
    if (shopName == null && rating == null) return
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        shopName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (onShopClick != null) Modifier.clickable(role = Role.Button, onClick = onShopClick)
                    else Modifier,
                ),
            )
        }
        rating?.let { CheckInRating(it) }
    }
}

@Composable
internal fun HelpfulButton(
    helpfulCount: Int,
    isHelpful: Boolean,
    onClick: (() -> Unit)?,
) {
    val description = stringResource(when {
        onClick == null -> Res.string.legacy_like_count
        isHelpful -> Res.string.legacy_like_remove_count
        else -> Res.string.legacy_like_add_count
    }, helpfulCount)

    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        CoffeePeekLikeIcon(isHelpful, contentDescription = null)
        Text(
            text = helpfulCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReviewHeader(
    review: Review, onEditClick: (() -> Unit)?, onReportClick: (() -> Unit)?,
    authorPhotoUrl: String?, dateLabel: String?,
) {
    val localizedLegacyReviewContentCommunityUser = stringResource(Res.string.legacy_review_content_community_user)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReviewAvatar(username = review.username, photoUrl = authorPhotoUrl)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = CpDimens.spacing3),
        ) {
            Text(
                text = review.username.ifBlank { localizedLegacyReviewContentCommunityUser },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (dateLabel != null || review.createdAt.isNotBlank()) Text(
                text = dateLabel ?: rememberReviewDisplayDate(review.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CheckInActions(onEditClick = onEditClick, onReportClick = onReportClick)
    }
}

@Composable
internal fun ReviewAvatar(username: String, photoUrl: String?) {
    val avatarModifier = Modifier.size(44.dp).clip(CircleShape)
    Box(
        modifier = avatarModifier.background(CpColor.GoldWarmSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = username.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CpColor.LightTextPrimary,
        )
        if (photoUrl != null) CpImage(data = photoUrl, modifier = avatarModifier)
    }
}

@Composable
internal fun CheckInRating(rating: ReviewRating) {
    CoffeePeekRating(rating.average, activeColor = CpColor.Primary,
        valueText = formatOneDecimal(rating.average), valueColor = MaterialTheme.colorScheme.onSurface,
        contentDescription = stringResource(Res.string.coffeepeek_legacy_rating_description,
            rating.coffee, rating.service, rating.place, formatOneDecimal(rating.average)))
}

@Composable private fun SharedReviewRatingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    androidx.compose.material3.Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CheckInRating(ReviewRating(4, 5, 4))
            EditableRatingCard(Res.drawable.checkin_rating_coffee, "Кофе", 4, {})
        }
    }
}
@Preview @Composable private fun SharedReviewRatingLightPreview() = SharedReviewRatingPreview(false)
@Preview @Composable private fun SharedReviewRatingDarkPreview() = SharedReviewRatingPreview(true)

private const val REVIEW_COLLAPSED_LINES = 4

/** Comment clamped to [REVIEW_COLLAPSED_LINES]; long ones get «Читать полностью» to expand in place. */
@Composable
private fun ReviewQuote(reviewId: String, comment: String, padToCollapsedLines: Boolean) {
    val localizedLegacyGroupedListDiscoveryFiltersCollapse = stringResource(Res.string.legacy_grouped_list_discovery_filters_collapse)
    val localizedLegacyReviewContentShopCheckinsReadMore = stringResource(Res.string.legacy_review_content_shop_checkins_read_more)

    var expanded by rememberSaveable(reviewId) { mutableStateOf(false) }
    var overflows by remember(comment) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else REVIEW_COLLAPSED_LINES,
            minLines = if (padToCollapsedLines && !expanded) REVIEW_COLLAPSED_LINES else 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.animateContentSize(),
        )
        if (overflows || expanded) {
            Text(
                text = if (expanded) localizedLegacyGroupedListDiscoveryFiltersCollapse else localizedLegacyReviewContentShopCheckinsReadMore,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = CpDimens.spacing1)
                    .clip(RoundedCornerShape(CpDimens.radiusSm))
                    .clickable(
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = !expanded }
                    .padding(vertical = CpDimens.spacing1),
            )
        }
    }
}

@Composable
internal fun rememberReviewDisplayDate(raw: String): String {
    val labels = reviewDateLabels()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(currentEpochMillis(), raw, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = currentEpochMillis()
                delay(60_000)
            }
        }
    }
    return remember(raw, now, labels) { formatReviewDisplayDate(raw, labels, now) }
}

internal data class ReviewDateLabels(
    val justNow: String,
    val ago: String,
    val minutes: List<String>,
    val hours: List<String>,
    val months: List<String>,
)

@Composable
internal fun reviewDateLabels() = ReviewDateLabels(
    stringResource(Res.string.legacy_review_just_now),
    stringResource(Res.string.legacy_review_ago),
    stringArrayResource(Res.array.legacy_minute_forms),
    stringArrayResource(Res.array.legacy_hour_forms),
    stringArrayResource(Res.array.legacy_review_months),
)

@OptIn(ExperimentalTime::class)
internal fun formatReviewDisplayDate(raw: String, labels: ReviewDateLabels, nowMillis: Long = currentEpochMillis()): String {
    val instant = parseServerInstant(raw.trim())
    val elapsed = instant?.let { nowMillis - it.toEpochMilliseconds() }
    if (elapsed != null && elapsed in 0 until 86_400_000L) {
        val minutes = elapsed / 60_000
        if (minutes == 0L) return labels.justNow
        val count = (if (minutes < 60) minutes else minutes / 60).toInt()
        val forms = if (minutes < 60) labels.minutes else labels.hours
        return "${pluralCountLabel(count, forms)} ${labels.ago}"
    }
    return reviewAbsoluteDate(raw, labels.months)
}

private fun reviewAbsoluteDate(raw: String, months: List<String>): String {
    val datePart = utcIsoToLocalDate(raw)
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val monthIndex = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return datePart
    val day = parts[2].trimStart('0').ifBlank { "0" }
    return "$day ${months[monthIndex - 1]} ${parts[0]}"
}

@Composable
private fun RatingValue(label: String, value: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(CpDimens.radiusSm))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = CpDimens.spacing2, vertical = CpDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Icon(CpIcons.StarFilled, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
        Text(value.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ReviewPhotoStrip(
    photoUrls: List<String>,
    modifier: Modifier = Modifier,
    thumbnailUrls: List<String> = photoUrls,
    singlePhotoAspectRatio: Float = 4f / 3f,
    originalUrls: List<String> = photoUrls,
    onPhotoClick: ((PhotoViewerImages, Int) -> Unit)? = null,
) {
    if (photoUrls.isEmpty()) return
    @Composable
    fun Photo(index: Int, photoModifier: Modifier) {
        val description = stringResource(Res.string.legacy_photo_position, index + 1, photoUrls.size)

        Box(
            photoModifier.clip(RoundedCornerShape(CpDimens.radiusMd))
                .then(if (onPhotoClick != null) Modifier.clickable(role = Role.Button) {
                    onPhotoClick(PhotoViewerImages(
                        photoUrls.mapIndexed { i, url -> thumbnailUrls.getOrNull(i) ?: url }, originalUrls,
                    ), index)
                } else Modifier)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                },
        ) {
            CpImage(
                data = thumbnailUrls.getOrNull(index) ?: photoUrls[index],
                highQualityData = photoUrls[index].takeIf(String::isNotBlank),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (index == 2 && photoUrls.size > 3) Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+${photoUrls.size - 3}", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
    }
    if (photoUrls.size == 1) {
        Photo(0, modifier.fillMaxWidth().aspectRatio(singlePhotoAspectRatio))
    } else Row(
        modifier.fillMaxWidth().aspectRatio(4f / 3f),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Photo(0, Modifier.weight(1f).fillMaxHeight())
        if (photoUrls.size == 2) Photo(1, Modifier.weight(1f).fillMaxHeight())
        else Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
            Photo(1, Modifier.weight(1f).fillMaxWidth())
            Photo(2, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
internal fun SavedDrinkLabel(name: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CpIcons.Coffee, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(name, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
