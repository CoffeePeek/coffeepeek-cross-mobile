package com.coffeepeek.feature.profile.impl.ui.compose.component

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_avatar_of
import com.coffeepeek.feature.profile.impl.resources.profile_edit
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
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
    onEditProfile: (() -> Unit)? = null,
    stats: PublicProfileStats? = null,
    onFollowersClick: () -> Unit = {},
    onAvatarClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(nickname, avatarUrl, image, onEditProfile, onAvatarClick)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("@${nickname.trimStart('@')}", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (stats != null) ProfileStats(stats, onFollowersClick)
            }
        }
        if (!about.isNullOrBlank()) Text(about, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onFollow != null) ProfileFollowButton(nickname, isFollowing, followEnabled, onFollow)
    }
}

@Composable
private fun ProfileAvatar(nickname: String, avatarUrl: String?, image: ProfileImage,
    onEditProfile: (() -> Unit)?, onAvatarClick: () -> Unit) {
    Box(Modifier.size(88.dp)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
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
        if (onEditProfile != null) FilledIconButton(
            onClick = onEditProfile, modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        ) { Icon(CoffeePeekIcons.Edit, stringResource(Res.string.profile_edit), Modifier.size(18.dp)) }
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
private fun ProfileStats(stats: PublicProfileStats, onFollowersClick: () -> Unit) {
    val items = listOf(stats.checkIns to stringResource(Res.string.profile_stat_check_ins),
        stats.coffeeShops to stringResource(Res.string.profile_stat_shops),
        stats.followers to stringResource(Res.string.profile_stat_followers))
    val followersLabel = stringResource(Res.string.profile_open_followers)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, (count, label) ->
            Column(
                Modifier.weight(1f).then(if (index == items.lastIndex) Modifier.clickable(role = Role.Button,
                    onClickLabel = followersLabel, onClick = onFollowersClick) else Modifier).padding(vertical = 4.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start)
            }
        }
    }
}

@Preview
@Composable
private fun ProfileHeaderPreview(dark: Boolean) = com.coffeepeek.core.designsystem.theme.CoffeePeekTheme(darkTheme = dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(16.dp)) {
            PublicProfileHeader("goloologo", null, "Варю, пью, фоткаю…", { _, _, _ -> },
                onEditProfile = {}, stats = PublicProfileStats(24, 6, 9, 3), onAvatarClick = {})
        }
    }
}

@Preview @Composable private fun ProfileHeaderLightPreview() = ProfileHeaderPreview(false)
@Preview @Composable private fun ProfileHeaderDarkPreview() = ProfileHeaderPreview(true)
