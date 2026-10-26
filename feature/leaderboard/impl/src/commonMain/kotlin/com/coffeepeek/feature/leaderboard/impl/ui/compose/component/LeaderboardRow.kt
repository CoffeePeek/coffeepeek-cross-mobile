package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.api.LeaderboardImage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardEntry
import com.coffeepeek.feature.leaderboard.impl.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardRow(row: LeaderboardEntry, image: LeaderboardImage, onOpen: () -> Unit, detailed: Boolean) {
    val open = stringResource(Res.string.leaderboard_open_profile, row.username)
    val click = if (row.authorSlug != null) Modifier.clickable(role = Role.Button, onClickLabel = open, onClick = onOpen)
        else Modifier
    val largeFont = LocalDensity.current.fontScale > 1.3f
    Column(Modifier.fillMaxWidth().then(click).semantics(mergeDescendants = true) {}
        .padding(vertical = CoffeePeekDimens.spacing2)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val rank = stringResource(Res.string.leaderboard_rank, row.rank)
            Text(row.rank.toString(), style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(min = 20.dp).semantics { contentDescription = rank })
            LeaderboardAvatar(row, image)
            Column(Modifier.weight(1f)) {
                Text(row.username, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (largeFont) LeaderboardPoints(row.points)
            }
            if (!largeFont) LeaderboardPoints(row.points)
        }
        if (detailed) Text(stringResource(Res.string.leaderboard_breakdown, row.additions, row.edits, row.checkIns),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = CoffeePeekDimens.spacing1))
    }
}

@Composable
private fun LeaderboardAvatar(row: LeaderboardEntry, image: LeaderboardImage) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(CoffeePeekColors.GoldWarmSoft).clearAndSetSemantics {},
        contentAlignment = Alignment.Center) {
        Text(row.username.trim().firstOrNull()?.uppercase().orEmpty(), color = CoffeePeekColors.LightTextPrimary,
            style = MaterialTheme.typography.titleSmall)
        row.avatarUrl?.let { image(it, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun LeaderboardPoints(points: Int) {
    Text(pluralStringResource(Res.plurals.leaderboard_points, points, points), style = MaterialTheme.typography.bodyMedium)
}

@Preview @Composable private fun LeaderboardRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardRow(LeaderboardEntry(1, "preview", "Maria", null, "maria", 21, 3, 2, 5), { _, _ -> }, {}, true)
}

@Preview @Composable private fun LeaderboardRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardRow(LeaderboardEntry(1, "preview", "Maria", null, "maria", 21, 3, 2, 5), { _, _ -> }, {}, true)
}

@Composable
private fun LeaderboardLargeFontPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.7f)) {
        LeaderboardRow(LeaderboardEntry(123, "preview", "Очень длинное имя участника", null, null, 123456, 0, 0, 0),
            { _, _ -> }, {}, true)
    }
}

@Preview @Composable private fun LeaderboardLargeFontLightPreview() = LeaderboardLargeFontPreview(false)
@Preview @Composable private fun LeaderboardLargeFontDarkPreview() = LeaderboardLargeFontPreview(true)
