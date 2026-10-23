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
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
            // At most three cards, so a plain scrolling row: all of them stay composed, the row keeps the height of the tallest card, and a card that
            // leaves or enters the screen no longer makes the page below jump (a lazy row measures only the visible cards).
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = contentPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                checkIns.take(3).forEachIndexed { index, checkIn ->
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
          }
            if (!isLoggedIn) Box(gutter) { ShopCheckInGuestPrompt(onSignIn, onRegister) }
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
