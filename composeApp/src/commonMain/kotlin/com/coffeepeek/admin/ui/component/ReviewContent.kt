package com.coffeepeek.admin.ui.component

import com.coffeepeek.domain.model.savedDrinkName

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.formatOneDecimal
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_atmosphere
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import coffeepeek.composeapp.generated.resources.checkin_rating_service
import coffeepeek.composeapp.generated.resources.drink_mock_cappuccino
import coffeepeek.composeapp.generated.resources.drink_mock_espresso
import coffeepeek.composeapp.generated.resources.drink_mock_matcha
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
import kotlin.time.Instant

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
    shopName: String? = null,
) {
    CheckInCard(modifier, blurContent) {
        ReviewHeader(review = review, onEditClick = onEditClick, onReportClick = onReportClick, authorPhotoUrl = authorPhotoUrl, dateLabel = dateLabel)

        val heading = shopName?.takeIf(String::isNotBlank) ?: review.header
        if (heading.isNotBlank()) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CheckInRating(review.rating)
        if (showDrinkBadge) savedDrinkName(review.drinkNameRu, review.drinkNameEn, review.customDrinkName, Locale.current.language)?.let {
            SavedDrinkBadge(it, review.drinkSlug)
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
    onPhotoClick: ((List<String>, Int) -> Unit)? = null,
    onHelpfulClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    isOwn: Boolean = true,
    fullVersion: Boolean = false,
    blurContent: Boolean = false,
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null && checkIn.shopId.isNotBlank()) Modifier.clickable(onClick = onClick) else Modifier)

    CheckInCard(cardModifier, blurContent) {
        CheckInHeader(checkIn = checkIn, isOwn = isOwn, onReportClick = onReportClick)
        if (showShopName) Text(checkIn.shopName.ifBlank { "Кофейня" },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        checkIn.rating?.let { CheckInRating(it) }
        savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)?.let {
            SavedDrinkBadge(it, checkIn.drinkSlug)
        }
        if (checkIn.note.isNotBlank()) {
            if (fullVersion) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            else ReviewQuote(checkIn.id, checkIn.note, padToCollapsedLines = false)
        }
        ReviewPhotoStrip(
            photoUrls = checkIn.photoUrls,
            thumbnailUrls = checkIn.photoThumbnailUrls,
            onPhotoClick = onPhotoClick,
        )
        if (checkIn.visibility == CheckInVisibility.Public || checkIn.helpfulCount > 0) {
            HelpfulButton(checkIn.helpfulCount, checkIn.isHelpfulByCurrentUser, onClick = onHelpfulClick)
        }
    }
}

@Composable
private fun CheckInHeader(checkIn: CheckIn, isOwn: Boolean, onReportClick: (() -> Unit)?) {
    val date = checkIn.createdAt
    val username = checkIn.username.ifBlank { if (isOwn) "Вы" else "Пользователь" }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReviewAvatar(username = username, photoUrl = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = CpDimens.spacing3),
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            if (date.isNotBlank()) Text(
                text = rememberReviewDisplayDate(date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CheckInActions(onEditClick = null, onReportClick = onReportClick)
    }
}

@Composable
internal fun HelpfulButton(
    helpfulCount: Int,
    isHelpful: Boolean,
    onClick: (() -> Unit)?,
) {
    val tint = if (isHelpful) CpColor.Error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = when {
                    onClick == null -> "Лайки: $helpfulCount"
                    isHelpful -> "Убрать лайк, лайков: $helpfulCount"
                    else -> "Поставить лайк, лайков: $helpfulCount"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Icon(
            imageVector = if (isHelpful) CpIcons.FavoriteFilled else CpIcons.Favorite,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
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
    val avatarModifier = Modifier.size(40.dp).clip(CircleShape)
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
    val filledStars = rating.average.toInt().coerceIn(0, 5)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Кофе: ${rating.coffee}, сервис: ${rating.service}, место: ${rating.place}. Средняя оценка ${formatOneDecimal(rating.average)} из 5"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (1..5).forEach { star ->
                Icon(
                    imageVector = if (star <= filledStars) CpIcons.StarFilled else CpIcons.StarOutline,
                    contentDescription = null,
                    tint = if (star <= filledStars) CpColor.Primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = formatOneDecimal(rating.average),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = CpDimens.spacing2),
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
            color = MaterialTheme.colorScheme.onSurface,
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

@Composable
internal fun rememberReviewDisplayDate(raw: String): String {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(currentEpochMillis(), raw, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = currentEpochMillis()
                delay(60_000)
            }
        }
    }
    return formatReviewDisplayDate(raw, now)
}

@OptIn(ExperimentalTime::class)
internal fun formatReviewDisplayDate(raw: String, nowMillis: Long = currentEpochMillis()): String {
    val trimmed = raw.trim()
    val instant = runCatching { Instant.parse(trimmed) }.getOrNull()
        ?: runCatching { Instant.parse("${trimmed}Z") }.getOrNull()
    if (instant != null) {
        val elapsed = nowMillis - instant.toEpochMilliseconds()
        if (elapsed in 0 until 86_400_000L) {
            val minutes = elapsed / 60_000
            if (minutes == 0L) return "только что"
            val count = if (minutes < 60) minutes else minutes / 60
            val forms = if (minutes < 60) listOf("минуту", "минуты", "минут") else listOf("час", "часа", "часов")
            val form = when {
                count % 100 in 11L..14L -> forms[2]
                count % 10 == 1L -> forms[0]
                count % 10 in 2L..4L -> forms[1]
                else -> forms[2]
            }
            return "$count $form назад"
        }
    }
    val datePart = utcIsoToLocalDate(raw)
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val month = when (parts[1].toIntOrNull()) {
        1 -> "янв."
        2 -> "февр."
        3 -> "мар."
        4 -> "апр."
        5 -> "мая"
        6 -> "июн."
        7 -> "июл."
        8 -> "авг."
        9 -> "сент."
        10 -> "окт."
        11 -> "нояб."
        12 -> "дек."
        else -> return datePart
    }
    return "${parts[2].trimStart('0').ifBlank { "0" }} $month ${parts[0]}"
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
) {
    if (photoUrls.isEmpty()) return
    @Composable
    fun Photo(index: Int, photoModifier: Modifier) {
        Box(
            photoModifier.clip(RoundedCornerShape(CpDimens.radiusMd))
                .then(if (onPhotoClick != null) Modifier.clickable(role = Role.Button) {
                    onPhotoClick(photoUrls, index)
                } else Modifier)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Фотография ${index + 1} из ${photoUrls.size}"
                },
        ) {
            CpImage(
                data = thumbnailUrls.getOrNull(index) ?: photoUrls[index],
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
        Photo(0, modifier.fillMaxWidth().aspectRatio(4f / 3f))
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
internal fun SavedDrinkBadge(name: String, slug: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CpDimens.radiusLg))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(CpDimens.spacing1),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(mockDrinkPhoto(name, slug)), contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(CpDimens.radiusMd)),
        )
        Text(name, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        // ponytail: the drink summary is static until a drink-detail route exists.
        Icon(CpIcons.ChevronRight, contentDescription = null,
            modifier = Modifier.padding(end = CpDimens.spacing2).size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

// ponytail: generated drink photos are placeholders; replace when the drink API provides image URLs.
internal fun mockDrinkPhoto(name: String, slug: String?): DrawableResource {
    val drink = "${slug.orEmpty()} $name".lowercase()
    return when {
        "matcha" in drink || "матч" in drink -> Res.drawable.drink_mock_matcha
        "espresso" in drink || "эспресс" in drink || "americano" in drink || "американо" in drink -> Res.drawable.drink_mock_espresso
        else -> Res.drawable.drink_mock_cappuccino
    }
}
