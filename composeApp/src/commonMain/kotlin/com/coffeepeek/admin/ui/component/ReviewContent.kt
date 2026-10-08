package com.coffeepeek.admin.ui.component

import com.coffeepeek.domain.model.savedDrinkName

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.admin.utils.formatOneDecimal
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_atmosphere
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import coffeepeek.composeapp.generated.resources.checkin_rating_service
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReviewFormField(
    label: String,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
        error?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
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
    BasicTextField(
        value = fieldValue.value,
        onValueChange = { updated ->
            val limited = updated.limitTextLength(maxLength)
            fieldValue.value = limited
            onValueChange(limited.text)
        },
        keyboardOptions = platformTextInputOptions(),
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
                if (value.isEmpty()) {
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Ваши оценки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Нажмите на звёзды, чтобы изменить оценку",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EditableRatingCard(
            image = Res.drawable.checkin_rating_coffee,
            label = "Кофе",
            rating = coffeeRating,
            onRatingChange = onCoffeeRatingChange,
        )
        EditableRatingCard(
            image = Res.drawable.checkin_rating_service,
            label = "Сервис",
            rating = serviceRating,
            onRatingChange = onServiceRatingChange,
        )
        EditableRatingCard(
            image = Res.drawable.checkin_rating_atmosphere,
            label = "Атмосфера",
            rating = placeRating,
            onRatingChange = onPlaceRatingChange,
        )
    }
}

@Composable
private fun EditableRatingCard(
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
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                (1..5).forEach { star ->
                    val icon: ImageVector = if (star <= rating) CpIcons.StarFilled else CpIcons.StarOutline
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onRatingChange(star) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "$label: выбрать $star из 5",
                            tint = if (star <= rating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewRatingSummary(rating: ReviewRating, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        RatingValue("Кофе", rating.coffee)
        RatingValue("Сервис", rating.service)
        RatingValue("Атмосфера", rating.place)
    }
}

/** Shared check-in presentation; legacy read models remain until the API migration. */
@Composable
private fun CheckInCard(
    modifier: Modifier,
    blurContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.then(if (blurContent) Modifier.blur(5.dp) else Modifier).padding(CpDimens.spacing3),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            content = content,
        )
    }
}

@Composable
fun ReviewDisplayCard(
    review: Review,
    modifier: Modifier = Modifier,
    onPhotoClick: ((List<String>, Int) -> Unit)? = null,
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
) {
    CheckInCard(modifier, blurContent) {
        ReviewHeader(review = review, onEditClick = onEditClick, onReportClick = onReportClick, authorPhotoUrl = authorPhotoUrl, dateLabel = dateLabel)

        if (review.header.isNotBlank()) {
            Text(
                text = review.header,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDrinkBadge) savedDrinkName(review.drinkNameRu, review.drinkNameEn, review.customDrinkName, Locale.current.language)?.let {
            SavedDrinkBadge(it)
        }
        if (review.comment.isNotBlank()) {
            if (fullVersion) Text(review.comment, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else ReviewQuote(review.id, review.comment, padToCollapsedLines = equalizeHeight)
        }

        ReviewPhotoStrip(
            photoUrls = review.photoUrls,
            onPhotoClick = onPhotoClick,
            tileSize = 144.dp,
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
    onPhotoClick: ((List<String>, Int) -> Unit)? = null,
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null && checkIn.shopId.isNotBlank()) Modifier.clickable(onClick = onClick) else Modifier)

    CheckInCard(cardModifier) {
        CheckInHeader(checkIn = checkIn, showShopName = showShopName)
        savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)?.let {
            SavedDrinkBadge(it)
        }
        if (checkIn.note.isNotBlank()) {
            ReviewQuote(checkIn.id, checkIn.note, padToCollapsedLines = false)
        }
        ReviewPhotoStrip(
            photoUrls = checkIn.photoUrls,
            thumbnailUrls = checkIn.photoThumbnailUrls,
            onPhotoClick = onPhotoClick,
            tileSize = 144.dp,
        )
    }
}

@Composable
private fun CheckInHeader(checkIn: CheckIn, showShopName: Boolean) {
    val date = checkIn.visitedAt.ifBlank { checkIn.createdAt }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CpIcons.Location,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = CpDimens.spacing3),
        ) {
            Text(
                text = if (showShopName) {
                    checkIn.shopName.ifBlank { "Кофейня" }
                } else {
                    "Посещение"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                checkIn.rating?.let { ReviewScoreRow(it.average, compact = true) }
                if (date.isNotBlank()) Text(
                    text = formatReviewDisplayDate(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun HelpfulButton(
    helpfulCount: Int,
    isHelpful: Boolean,
    onClick: (() -> Unit)?,
    showLabel: Boolean = true,
) {
    val tint = if (isHelpful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .height(CpDimens.buttonHeight)
            .clip(RoundedCornerShape(percent = 50))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Icon(
            imageVector = if (isHelpful) CpIcons.FavoriteFilled else CpIcons.Favorite,
            contentDescription = when {
                onClick == null -> "Отметки «полезно»: $helpfulCount"
                isHelpful -> "Убрать отметку «полезно»"
                else -> "Отметить как полезный"
            },
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = when {
                !showLabel -> helpfulCount.toString()
                helpfulCount > 0 -> "Полезно · $helpfulCount"
                else -> "Полезно"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Composable
private fun ReviewHeader(
    review: Review, onEditClick: (() -> Unit)?, onReportClick: (() -> Unit)?,
    authorPhotoUrl: String?, dateLabel: String?,
) {
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
                text = review.username.ifBlank { "Пользователь" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                ReviewScoreRow(review.rating.average, compact = true)
                if (dateLabel != null || review.createdAt.isNotBlank()) {
                    Text(
                        text = dateLabel ?: formatReviewDisplayDate(review.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CheckInActions(onEditClick = onEditClick, onReportClick = onReportClick)
    }
}

@Composable
private fun ReviewAvatar(username: String, photoUrl: String?) {
    val avatarModifier = Modifier.size(44.dp).clip(CircleShape)
    Box(
        modifier = avatarModifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            shape = CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = username.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (photoUrl != null) CpImage(data = photoUrl, modifier = avatarModifier.padding(2.dp))
    }
}

@Composable
private fun ReviewScoreRow(average: Double, compact: Boolean = false) {
    val filledStars = average.toInt().coerceIn(0, 5)
    Row(
        modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            (1..if (compact) 1 else 5).forEach { star ->
                Icon(
                    imageVector = if (compact || star <= filledStars) CpIcons.StarFilled else CpIcons.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (compact) 16.dp else 20.dp),
                )
            }
        }
        Text(
            text = formatOneDecimal(average),
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = if (compact) 3.dp else CpDimens.spacing2),
        )
    }
}

private const val REVIEW_COLLAPSED_LINES = 4

/** Comment clamped to [REVIEW_COLLAPSED_LINES]; long ones get «Читать полностью» to expand in place. */
@Composable
private fun ReviewQuote(reviewId: String, comment: String, padToCollapsedLines: Boolean) {
    var expanded by rememberSaveable(reviewId) { mutableStateOf(false) }
    var overflows by remember(comment) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else REVIEW_COLLAPSED_LINES,
            minLines = if (padToCollapsedLines && !expanded) REVIEW_COLLAPSED_LINES else 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.animateContentSize(),
        )
        if (overflows || expanded) {
            Text(
                text = if (expanded) "Свернуть" else "Читать полностью",
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

internal fun formatReviewDisplayDate(raw: String): String {
    val datePart = utcIsoToLocalDate(raw)
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val month = when (parts[1].toIntOrNull()) {
        1 -> "января"
        2 -> "февраля"
        3 -> "марта"
        4 -> "апреля"
        5 -> "мая"
        6 -> "июня"
        7 -> "июля"
        8 -> "августа"
        9 -> "сентября"
        10 -> "октября"
        11 -> "ноября"
        12 -> "декабря"
        else -> return datePart
    }
    return "${parts[2].trimStart('0').ifBlank { "0" }} $month ${parts[0]} г."
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
    // Same order as photoUrls: tiles show these, a click still hands out the full photo.
    thumbnailUrls: List<String> = photoUrls,
    onPhotoClick: ((List<String>, Int) -> Unit)? = null,
    tileSize: Dp = 72.dp,
) {
    if (photoUrls.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        itemsIndexed(photoUrls) { index, url ->
            CpImage(
                data = thumbnailUrls.getOrNull(index) ?: url,
                contentDescription = "Фотография ${index + 1} из ${photoUrls.size}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(tileSize)
                    .clip(RoundedCornerShape(CpDimens.radiusSm))
                    .then(
                        if (onPhotoClick != null) {
                            Modifier.clickable { onPhotoClick(photoUrls, index) }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
internal fun SavedDrinkBadge(name: String, showLabel: Boolean = true) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CpIcons.Coffee, contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        Text(if (showLabel) "Напиток: $name" else name, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
