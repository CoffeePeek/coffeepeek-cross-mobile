package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.data.profileCheckInDrink
import com.coffeepeek.feature.profile.impl.ui.data.profileCheckInPhotos
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.intl.Locale
import kotlin.math.round

@Composable
internal fun PublicProfileCheckInCard(
    row: PublicProfileCheckIn, author: ProfileAuthor, slots: ProfileSlots,
    onShop: (() -> Unit)?, onPhoto: (List<String>, List<String>, Int) -> Unit,
    onBlock: (() -> Unit)?, onReport: (() -> Unit)?,
) {
    val name = row.authorName.ifBlank { author.name }
    CoffeePeekTimelineContent(row.note,
        header = {
            CoffeePeekTimelineHeader(name, slots.displayDate(row.createdAt), avatar = { modifier ->
                (row.authorAvatarUrl ?: author.avatarUrl)?.let { slots.image(it, null, modifier) }
            }, actions = { ProfileCheckInActions(onBlock, onReport) })
        }, heading = { ProfileCheckInHeading(row, onShop) },
        photos = { ProfileCheckInPhotos(row, slots, onPhoto) },
        footer = {
            CoffeePeekTimelineFooter(row.helpfulCount, row.isHelpfulByCurrentUser,
                stringResource(Res.string.profile_check_in_likes, row.helpfulCount), row.drink.profileCheckInDrink(Locale.current.language))
        })
}

@Composable
private fun ProfileCheckInHeading(row: PublicProfileCheckIn, onShop: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (row.isHome) ProfileCheckInPreparation(row.preparation)
        else {
            Text(row.venue.ifBlank { stringResource(Res.string.profile_favorite_shop) },
                Modifier.then(onShop?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            row.scores?.let { scores ->
                val tenths = round(scores.average * 10).toInt()
                val label = "${tenths / 10}.${tenths % 10}"
                CoffeePeekRating(scores.average, activeColor = CoffeePeekColors.Primary, valueText = label,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    contentDescription = stringResource(Res.string.profile_check_in_rating, scores.coffee, scores.service, scores.place, label))
            }
        }
    }
}

@Composable
private fun ProfileCheckInPhotos(row: PublicProfileCheckIn, slots: ProfileSlots, onPhoto: (List<String>, List<String>, Int) -> Unit) {
    val photos = row.profileCheckInPhotos()
    val descriptions = photos.originals.indices.map { stringResource(Res.string.profile_check_in_photo, it + 1, photos.originals.size) }
    CoffeePeekPhotoStrip(photos.originals.size, { descriptions[it] }, { onPhoto(photos.previews, photos.originals, it) },
        photoEnabled = photos::canOpen) { index, modifier ->
        slots.progressiveImage(photos.display(index), photos.upgrade(index), modifier)
    }
}

@Composable
private fun ProfileCheckInActions(onBlock: (() -> Unit)?, onReport: (() -> Unit)?) {
    if (onBlock == null && onReport == null) return
    val label = stringResource(Res.string.profile_check_in_actions)
    val actions = buildList {
        onBlock?.let { add(CoffeePeekMenuAction(stringResource(Res.string.profile_block_author), CoffeePeekIcons.Block,
            destructive = true, nativeIcon = CoffeePeekMenuIcon.Block, onClick = it)) }
        onReport?.let { add(CoffeePeekMenuAction(stringResource(Res.string.profile_check_in_report), CoffeePeekIcons.Error,
            nativeIcon = CoffeePeekMenuIcon.Report, onClick = it)) }
    }
    CoffeePeekActionMenu(actions, menuDescription = label) { open ->
        IconButton(open) { Icon(CoffeePeekIcons.MoreHorizontal, label, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun ProfileCheckInPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val slots = ProfileSlots({ _, _, _ -> }, { _, _, _, _ -> }, { _, _ -> })
    PublicProfileCheckInCard(PublicProfileCheckIn("demo", "coffee", "CoffeePeek", "Кофе", null, 4.5,
        "Сбалансированный кофе с ягодными нотами.", "14 октября 2026", emptyList()),
        ProfileAuthor("author", "anna", "Анна"), slots, {}, { _, _, _ -> }, {}, {})
}
@Preview @Composable private fun ProfileCheckInLightPreview() = ProfileCheckInPreview(false)
@Preview @Composable private fun ProfileCheckInDarkPreview() = ProfileCheckInPreview(true)
