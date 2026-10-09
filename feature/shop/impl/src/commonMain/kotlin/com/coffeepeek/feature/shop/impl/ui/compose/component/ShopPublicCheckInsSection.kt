package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_public_title
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_empty
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
    onOpenPhoto: (List<String>, Int) -> Unit,
    onVote: (String) -> Unit,
    onReport: (String) -> Unit,
    onOpenAll: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.shop_checkins_public_title), Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium)
            if (checkIns.isNotEmpty()) TextButton(onClick = onOpenAll) {
                Text(stringResource(Res.string.shop_checkins_show_all))
            }
        }
        if (checkIns.isEmpty()) Text(stringResource(Res.string.shop_checkins_empty, shopTitle))
        else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                itemsIndexed(checkIns.take(3), key = { _, item -> item.id }) { index, checkIn ->
                    val hidden = !isLoggedIn && index > 0
                    if (hidden) {
                        ShopCheckInGuestPlaceholder(Modifier.width(300.dp))
                        return@itemsIndexed
                    }
                    val own = checkIn.id in ownCheckInIds ||
                        (currentUserId?.takeIf(String::isNotBlank)?.let { it == checkIn.userId } == true)
                    ShopCheckInCard(
                        checkIn = checkIn,
                        onOpenPhoto = onOpenPhoto,
                        modifier = Modifier.width(300.dp),
                        showAuthor = true, showHelpful = true,
                        onVote = if (isLoggedIn && !own && pendingVoteId == null)
                            ({ onVote(checkIn.id) }) else null,
                        onReport = if (isLoggedIn && !own) ({ onReport(checkIn.id) }) else null,
                    )
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
