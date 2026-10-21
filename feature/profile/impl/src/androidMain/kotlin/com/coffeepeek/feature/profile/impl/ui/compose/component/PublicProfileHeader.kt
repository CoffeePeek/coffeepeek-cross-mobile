package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
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
import com.coffeepeek.feature.profile.impl.R

@OptIn(ExperimentalLayoutApi::class)
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
    onAvatarClick: () -> Unit,
) {
    val handle = nickname.trimStart('@')
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(92.dp)) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CoffeePeekColors.GoldWarm, CoffeePeekColors.Primary)))
                    .then(if (!avatarUrl.isNullOrBlank()) Modifier.clickable(role = Role.Button,
                        onClickLabel = stringResource(R.string.profile_open_avatar), onClick = onAvatarClick) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    nickname.trimStart('@').take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.headlineLarge,
                    color = CoffeePeekColors.LightTextOnPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!avatarUrl.isNullOrBlank()) image(avatarUrl, stringResource(R.string.profile_avatar_of, handle), Modifier.fillMaxSize())
            }
            if (onEditProfile != null) FilledIconButton(
                onClick = onEditProfile,
                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) { Icon(CoffeePeekIcons.Edit, contentDescription = stringResource(R.string.profile_edit), modifier = Modifier.size(16.dp)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "@$handle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onFollow != null) {
                    val description = stringResource(if (isFollowing) R.string.profile_unfollow_description else R.string.profile_follow_description, handle)
                    OutlinedButton(
                        onClick = onFollow,
                        enabled = followEnabled,
                        modifier = Modifier.semantics { contentDescription = description },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text(stringResource(if (isFollowing) R.string.profile_following else R.string.profile_follow), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (!about.isNullOrBlank()) Text(about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PublicProfileStatsCard(stats: PublicProfileStats, onFollowersClick: () -> Unit = {}) {
    val items = listOf(
        stats.checkIns to stringResource(R.string.profile_stat_check_ins), stats.coffeeShops to stringResource(R.string.profile_stat_shops),
        stats.roasters to stringResource(R.string.profile_stat_roasters), stats.followers to stringResource(R.string.profile_stat_followers),
    )
    val followersLabel = stringResource(R.string.profile_open_followers)
    ProfileCard {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, (count, label) ->
                if (index > 0) Box(Modifier.width(1.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(
                    Modifier.weight(1f).then(
                        if (index == 3) Modifier.clickable(role = Role.Button, onClickLabel = followersLabel, onClick = onFollowersClick) else Modifier,
                    ).padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2,
                    )
                }
            }
        }
    }
}
