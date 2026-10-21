package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
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
    overallRating: Double? = null,
    checkInCount: Int = checkIns.size,
    formatDate: (String) -> String = { it.take(10) },
    isRefreshing: Boolean = false,
    onOpenAuthor: (String) -> Unit = {},
    onBlockAuthor: (String) -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShopSectionTitle(stringResource(Res.string.shop_checkins_public_title), Modifier.weight(1f))
            if (checkIns.isNotEmpty()) IconButton(onClick = onOpenAll) {
                Icon(CoffeePeekIcons.ChevronRight, stringResource(Res.string.shop_checkins_show_all),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (checkIns.isEmpty()) ShopCheckInsEmptyState(shopTitle)
        else {
          ShopCheckInRatingsOverview(checkIns, overallRating, checkInCount)
          BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds()) {
            val multiple = checkIns.size > 1
            val spacing = if (multiple) CoffeePeekDimens.spacing3 else 0.dp
            val width = if (!isLoggedIn && multiple) (maxWidth - spacing - 56.dp).coerceAtLeast(1.dp).coerceAtMost(320.dp)
                else if (multiple) maxWidth.coerceAtMost(320.dp) else maxWidth
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                itemsIndexed(checkIns.take(3), key = { _, item -> item.id }) { index, checkIn ->
                    val hidden = !isLoggedIn && index > 0
                    if (hidden) {
                        ShopCheckInGuestPlaceholder(Modifier.width(width))
                        return@itemsIndexed
                    }
                    val own = checkIn.id in ownCheckInIds ||
                        (currentUserId?.takeIf(String::isNotBlank)?.let { it == checkIn.userId } == true)
                    ShopCheckInCard(
                        checkIn = checkIn,
                        onOpenPhoto = { _, index -> onOpenPhoto(checkIn, index) },
                        modifier = Modifier.width(width),
                        showAuthor = true, showHelpful = true,
                        isOwn = own, isVoting = pendingVoteId == checkIn.id,
                        shopTitle = shopTitle, dateLabel = formatDate(checkIn.createdAt),
                        onVote = if (isLoggedIn && pendingVoteId == null && !isRefreshing)
                            ({ onVote(checkIn.id) }) else null,
                        onReport = if (isLoggedIn && !own) ({ onReport(checkIn.id) }) else null,
                        onBlockAuthor = checkIn.blockableAuthor(own, isLoggedIn, currentUserId)
                            ?.takeUnless { isRefreshing }?.let { { onBlockAuthor(checkIn.id) } },
                        onOpenAuthor = checkIn.authorSlug?.takeIf(String::isNotBlank)?.let { { onOpenAuthor(checkIn.id) } },
                    )
                }
            }
          }
            if (!isLoggedIn) ShopCheckInGuestPrompt(onSignIn, onRegister)
        }
    }
}

@Preview @Composable private fun ShopPublicCheckInsSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopPublicCheckInsSection(listOf(previewCheckIn(), previewCheckIn().copy(id = "visit-2")),
        "Кофейня", false, null, emptySet(), null, { _, _ -> }, {}, {}, {}, {}, {})
}

@Preview @Composable private fun ShopPublicCheckInsSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopPublicCheckInsSection(listOf(previewCheckIn()), "Кофейня", true, "user-2",
        emptySet(), null, { _, _ -> }, {}, {}, {}, {}, {})
}
