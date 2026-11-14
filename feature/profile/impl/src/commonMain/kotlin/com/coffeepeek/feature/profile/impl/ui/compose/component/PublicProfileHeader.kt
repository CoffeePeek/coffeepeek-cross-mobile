package com.coffeepeek.feature.profile.impl.ui.compose.component

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_avatar_of
import com.coffeepeek.feature.profile.impl.resources.profile_follow
import com.coffeepeek.feature.profile.impl.resources.profile_follow_description
import com.coffeepeek.feature.profile.impl.resources.profile_following
import com.coffeepeek.feature.profile.impl.resources.profile_open_avatar
import com.coffeepeek.feature.profile.impl.resources.profile_open_followers
import com.coffeepeek.feature.profile.impl.resources.profile_stat_check_ins
import com.coffeepeek.feature.profile.impl.resources.profile_stat_followers
import com.coffeepeek.feature.profile.impl.resources.profile_stat_shops
import com.coffeepeek.feature.profile.impl.resources.profile_unfollow_description
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.impl.resources.*

@Composable
internal fun PublicProfileHeader(
    nickname: String,
    avatarUrl: String?,
    about: String?,
    image: ProfileImage,
    isFollowing: Boolean = false,
    followEnabled: Boolean = true,
    onFollow: (() -> Unit)? = null,
    stats: PublicProfileStats? = null,
    onFollowersClick: (() -> Unit)? = null,
    onShopsClick: (() -> Unit)? = null,
    onRoastersClick: (() -> Unit)? = null,
    onAvatarClick: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val statsWidth = (if (stats?.roasters == 0) 168.dp else 220.dp) * LocalDensity.current.fontScale
        val inlineStats = maxWidth >= statsWidth + 68.dp
        val avatarSize = if (inlineStats) (maxWidth - statsWidth - 12.dp).coerceIn(56.dp, 88.dp) else 88.dp
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(nickname, avatarUrl, image, onAvatarClick, avatarSize)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("@${nickname.trimStart('@')}", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (stats != null && inlineStats) ProfileStats(stats, onFollowersClick, onShopsClick, onRoastersClick)
                }
            }
            if (stats != null && !inlineStats) Box(Modifier.horizontalScroll(rememberScrollState())) {
                ProfileStats(stats, onFollowersClick, onShopsClick, onRoastersClick, Modifier.width(maxOf(availableWidth, statsWidth)))
            }
            if (!about.isNullOrBlank()) Text(about, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onFollow != null) ProfileFollowButton(nickname, isFollowing, followEnabled, onFollow)
        }
    }
}

@Composable
private fun ProfileAvatar(nickname: String, avatarUrl: String?, image: ProfileImage,
    onAvatarClick: () -> Unit, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(CoffeePeekColors.GoldWarm, CoffeePeekColors.Primary)))
            .then(if (!avatarUrl.isNullOrBlank()) Modifier.clickable(role = Role.Button,
                onClickLabel = stringResource(Res.string.profile_open_avatar), onClick = onAvatarClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(nickname.trimStart('@').take(1).uppercase().ifBlank { "?" },
            style = MaterialTheme.typography.headlineLarge, color = CoffeePeekColors.LightTextOnPrimary,
            fontWeight = FontWeight.SemiBold)
        if (!avatarUrl.isNullOrBlank()) image(avatarUrl,
            stringResource(Res.string.profile_avatar_of, nickname.trimStart('@')), Modifier.fillMaxSize())
    }
}

@Composable
private fun ProfileFollowButton(nickname: String, isFollowing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (isFollowing) Res.string.profile_unfollow_description else Res.string.profile_follow_description,
        nickname.trimStart('@'))
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
    ) { Text(stringResource(if (isFollowing) Res.string.profile_following else Res.string.profile_follow),
        style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun ProfileStats(stats: PublicProfileStats, onFollowersClick: (() -> Unit)?,
    onShopsClick: (() -> Unit)?, onRoastersClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val checkIns = stringResource(Res.string.profile_stat_check_ins)
    val shops = stringResource(Res.string.profile_stat_shops)
    val roasters = stringResource(Res.string.profile_stat_roasters)
    val followers = stringResource(Res.string.profile_stat_followers)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ProfileStat(stats.checkIns, checkIns, Modifier.weight(checkIns.length.toFloat()))
        ProfileStat(stats.coffeeShops, shops, Modifier.weight(shops.length.toFloat()),
            onShopsClick, stringResource(Res.string.profile_menu_submitted_shops))
        if (stats.roasters > 0) ProfileStat(stats.roasters, roasters, Modifier.weight(roasters.length.toFloat()),
            onRoastersClick, stringResource(Res.string.profile_menu_submitted_roasters))
        ProfileStat(stats.followers, followers, Modifier.weight(followers.length.toFloat()),
            onFollowersClick, stringResource(Res.string.profile_open_followers))
    }
}

@Preview
@Composable
private fun ProfileStat(count: Int, label: String, modifier: Modifier, onClick: (() -> Unit)? = null,
    actionLabel: String = label) {
    Column(modifier.heightIn(min = 48.dp).then(if (onClick == null) Modifier else
        Modifier.clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileHeaderPreview(dark: Boolean, own: Boolean = true, width: Dp = 390.dp,
    roasters: Int = 3, fontScale: Float = 1f) = com.coffeepeek.core.designsystem.theme.CoffeePeekTheme(darkTheme = dark) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        Surface(Modifier.width(width), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                PublicProfileHeader("Arsen", null, "Фоткаю, пью, варю… Ладно, шучу, не варю", { _, _, _ -> },
                    stats = PublicProfileStats(19, 27, roasters, 1),
                    onShopsClick = if (own) ({}) else null, onRoastersClick = if (own) ({}) else null,
                    onFollowersClick = {}, onAvatarClick = {})
            }
        }
    }
}

@Preview @Composable private fun ProfileHeaderLightPreview() = ProfileHeaderPreview(false)
@Preview @Composable private fun ProfileHeaderDarkPreview() = ProfileHeaderPreview(true)
@Preview @Composable private fun PublicProfileHeaderLightPreview() = ProfileHeaderPreview(false, false)
@Preview @Composable private fun PublicProfileHeaderDarkPreview() = ProfileHeaderPreview(true, false)
@Preview @Composable private fun ProfileHeaderNarrowLightPreview() = ProfileHeaderPreview(false, width = 320.dp)
@Preview @Composable private fun ProfileHeaderNarrowDarkPreview() = ProfileHeaderPreview(true, width = 320.dp)
@Preview @Composable private fun ProfileHeaderNoRoastersLightPreview() = ProfileHeaderPreview(false, roasters = 0)
@Preview @Composable private fun ProfileHeaderNoRoastersDarkPreview() = ProfileHeaderPreview(true, roasters = 0)
@Preview @Composable private fun ProfileHeaderLargeFontLightPreview() = ProfileHeaderPreview(false, width = 320.dp, fontScale = 1.5f)
@Preview @Composable private fun ProfileHeaderLargeFontDarkPreview() = ProfileHeaderPreview(true, width = 320.dp, fontScale = 1.5f)
