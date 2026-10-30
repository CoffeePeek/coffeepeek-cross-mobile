package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.impl.ui.data.blockableAuthor
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_public_title
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_show_all
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopPublicCheckInsSection(
    checkIns: List<ShopCheckIn>,
    shopTitle: String,
    isLoggedIn: Boolean,
    currentUserId: String?,
    ownCheckInIds: Set<String>,
    pendingVoteId: String?,
    onOpenPhoto: (ShopCheckIn, Int) -> Unit,
    onVote: (String) -> Unit,
    onReport: (String) -> Unit,
    onOpenAll: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 0.dp,
    overallRating: Double? = null,
    checkInCount: Int = checkIns.size,
    formatDate: (String) -> String = { it.take(10) },
    isRefreshing: Boolean = false,
    onOpenAuthor: (String) -> Unit = {},
    onBlockAuthor: (String) -> Unit = {},
) {
    // [contentPadding] is the screen gutter: the cards scroll to the edges of the screen and only the first and the last keep the gutter.
    val gutter = Modifier.padding(horizontal = contentPadding)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Row(gutter.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShopSectionTitle(stringResource(Res.string.shop_checkins_public_title), Modifier.weight(1f))
            if (checkIns.isNotEmpty()) IconButton(onClick = onOpenAll) {
                Icon(CoffeePeekIcons.ChevronRight, stringResource(Res.string.shop_checkins_show_all),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (checkIns.isEmpty()) ShopCheckInsEmptyState(shopTitle, gutter)
        else {
          Box(gutter) { ShopCheckInRatingsOverview(checkIns, overallRating, checkInCount) }
          BoxWithConstraints(Modifier.fillMaxWidth()) {
            val multiple = checkIns.size > 1
            val spacing = if (multiple) CoffeePeekDimens.spacing3 else 0.dp
            val available = maxWidth - contentPadding * 2
            val width = if (!isLoggedIn && multiple) (available - spacing - 56.dp).coerceAtLeast(1.dp).coerceAtMost(320.dp)
                else if (multiple) available.coerceAtMost(320.dp) else available
            val cards = checkIns.take(3)
            ShopCheckInCarousel(cards.size, width, spacing, contentPadding, maxWidth) { index ->
                val checkIn = cards[index]
                key(checkIn.id) {
                    val hidden = !isLoggedIn && index > 0
                    if (hidden) ShopCheckInGuestPlaceholder(Modifier.width(width))
                    else ShopPublicCheckInItem(
                        checkIn = checkIn,
                        width = width,
                        shopTitle = shopTitle,
                        isLoggedIn = isLoggedIn,
                        currentUserId = currentUserId,
                        isOwnCheckIn = checkIn.id in ownCheckInIds,
                        isVoting = pendingVoteId == checkIn.id,
                        hasPendingVote = pendingVoteId != null,
                        isRefreshing = isRefreshing,
                        formatDate = formatDate,
                        onOpenPhoto = onOpenPhoto,
                        onVote = onVote,
                        onReport = onReport,
                        onOpenAuthor = onOpenAuthor,
                        onBlockAuthor = onBlockAuthor,
                    )
                }
            }
          }
            if (!isLoggedIn) Box(gutter) { ShopCheckInGuestPrompt(onSignIn, onRegister) }
        }
    }
}

/**
 * The height the carousel should have: that of the tallest card that is at least partly on screen, or null until every card has been measured.
 * All values are pixels; [scroll] is the horizontal scroll offset and [padding] the start padding of the row.
 */
internal fun carouselTargetHeight(heights: List<Int?>, scroll: Int, viewport: Int, card: Int, spacing: Int, padding: Int): Int? {
    if (heights.isEmpty() || heights.any { it == null }) return null
    var target = 0
    heights.forEachIndexed { index, height ->
        val left = padding + index * (card + spacing) - scroll
        if (left + card > 0 && left < viewport) target = maxOf(target, height ?: 0)
    }
    return target.takeIf { it > 0 } ?: heights.maxOf { it ?: 0 }
}

/**
 * At most three cards, so a plain scrolling row in which every card stays composed. Its height follows the cards that are on screen and is animated:
 * a short card alone is not stretched to the tallest one, and a tall one entering or leaving does not make the page below jump. Each card is measured at
 * its natural height; before all of them are measured the row simply wraps the tallest.
 */
@Composable
private fun ShopCheckInCarousel(
    count: Int,
    cardWidth: Dp,
    spacing: Dp,
    contentPadding: Dp,
    viewportWidth: Dp,
    card: @Composable (index: Int) -> Unit,
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val heights = remember(count) { mutableStateListOf<Int?>().apply { repeat(count) { add(null) } } }
    val target by remember(count, cardWidth, spacing, contentPadding, viewportWidth) {
        derivedStateOf {
            with(density) {
                carouselTargetHeight(heights.toList(), scroll.value, viewportWidth.roundToPx(), cardWidth.roundToPx(), spacing.roundToPx(), contentPadding.roundToPx())
            }
        }
    }
    val height = remember { Animatable(0f) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        val value = target?.toFloat() ?: return@LaunchedEffect
        if (!ready) { height.snapTo(value); ready = true } else height.animateTo(value, tween(durationMillis = 250))
    }
    Row(
        Modifier.then(if (ready) Modifier.height(with(density) { height.value.toDp() }).clipToBounds() else Modifier)
            .horizontalScroll(scroll).padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(count) { index ->
            Box(
                Modifier.width(cardWidth).wrapContentHeight(Alignment.Top, unbounded = true)
                    .onSizeChanged { size -> if (heights.getOrNull(index) != size.height) heights[index] = size.height },
            ) { card(index) }
        }
    }
}

/** Own item scope: a vote on one card must not recompose its neighbours. */
@Composable
private fun ShopPublicCheckInItem(
    checkIn: ShopCheckIn,
    width: Dp,
    shopTitle: String,
    isLoggedIn: Boolean,
    currentUserId: String?,
    isOwnCheckIn: Boolean,
    isVoting: Boolean,
    hasPendingVote: Boolean,
    isRefreshing: Boolean,
    formatDate: (String) -> String,
    onOpenPhoto: (ShopCheckIn, Int) -> Unit,
    onVote: (String) -> Unit,
    onReport: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    onBlockAuthor: (String) -> Unit,
) {
    val own = isOwnCheckIn ||
        (currentUserId?.takeIf(String::isNotBlank)?.let { it == checkIn.userId } == true)
    ShopCheckInCard(
        checkIn = checkIn,
        onOpenPhoto = { _, index -> onOpenPhoto(checkIn, index) },
        modifier = Modifier.width(width),
        showAuthor = true, showHelpful = true,
        isOwn = own, isVoting = isVoting,
        shopTitle = shopTitle, dateLabel = formatDate(checkIn.createdAt),
        onVote = if (isLoggedIn && !hasPendingVote && !isRefreshing) ({ onVote(checkIn.id) }) else null,
        onReport = if (isLoggedIn && !own) ({ onReport(checkIn.id) }) else null,
        onBlockAuthor = checkIn.blockableAuthor(own, isLoggedIn, currentUserId)
            ?.takeUnless { isRefreshing }?.let { { onBlockAuthor(checkIn.id) } },
        onOpenAuthor = checkIn.authorSlug?.takeIf(String::isNotBlank)?.let { { onOpenAuthor(checkIn.id) } },
    )
}

@Preview @Composable private fun ShopPublicCheckInsSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopPublicCheckInsSection(listOf(previewCheckIn(), previewCheckIn().copy(id = "visit-2")),
        "Кофейня", false, null, emptySet(), null, { _, _ -> }, {}, {}, {}, {}, {})
}

@Preview @Composable private fun ShopPublicCheckInsSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopPublicCheckInsSection(listOf(previewCheckIn()), "Кофейня", true, "user-2",
        emptySet(), null, { _, _ -> }, {}, {}, {}, {}, {})
}
