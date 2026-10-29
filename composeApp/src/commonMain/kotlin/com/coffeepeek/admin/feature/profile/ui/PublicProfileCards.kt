package com.coffeepeek.admin.feature.profile.ui

import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages

import org.jetbrains.compose.resources.stringResource
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.profile_open_avatar
import coffeepeek.composeapp.generated.resources.profile_avatar_of
import coffeepeek.composeapp.generated.resources.profile_edit
import coffeepeek.composeapp.generated.resources.profile_unfollow_description
import coffeepeek.composeapp.generated.resources.profile_follow_description
import coffeepeek.composeapp.generated.resources.profile_following
import coffeepeek.composeapp.generated.resources.profile_follow
import coffeepeek.composeapp.generated.resources.profile_stat_check_ins
import coffeepeek.composeapp.generated.resources.profile_stat_shops
import coffeepeek.composeapp.generated.resources.profile_stat_followers
import coffeepeek.composeapp.generated.resources.profile_open_followers
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.feature.checkin.api.CheckInTimelineContent
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import com.coffeepeek.domain.model.CheckIn

@Composable
internal fun PublicProfileHeader(
    nickname: String,
    avatarUrl: String?,
    about: String?,
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
            ProfileAvatar(nickname, avatarUrl, onEditProfile, onAvatarClick)
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
private fun ProfileAvatar(nickname: String, avatarUrl: String?,
    onEditProfile: (() -> Unit)?, onAvatarClick: () -> Unit) {
    Box(Modifier.size(88.dp)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(Brush.linearGradient(listOf(CpColor.GoldWarm, CpColor.Primary)))
                .then(if (!avatarUrl.isNullOrBlank()) Modifier.clickable(role = Role.Button,
                    onClickLabel = stringResource(Res.string.profile_open_avatar), onClick = onAvatarClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(nickname.trimStart('@').take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.headlineLarge, color = CpColor.DarkTextOnPrimary,
                fontWeight = FontWeight.SemiBold)
            if (!avatarUrl.isNullOrBlank()) CoffeePeekImage(data = avatarUrl, contentDescription =
                stringResource(Res.string.profile_avatar_of, nickname.trimStart('@')),
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        if (onEditProfile != null) FilledIconButton(
            onClick = onEditProfile, modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        ) { Icon(CpIcons.Edit, stringResource(Res.string.profile_edit), Modifier.size(18.dp)) }
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

@Composable
internal fun PublicProfileCheckInCard(
    checkIn: PublicProfileCheckIn,
    onShopClick: () -> Unit,
    onPhotoClick: (com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages, Int) -> Unit,
    authorName: String = "",
    authorAvatarUrl: String? = null,
    isOwnProfile: Boolean = false,
    author: com.coffeepeek.feature.authorblocking.api.AuthorReference? = null,
) {
    val source = checkIn.source ?: checkIn.legacyPresentation()
    CheckInTimelineContent(
        checkIn = source.copy(
            username = source.username.ifBlank { authorName },
            authorAvatarUrl = source.authorAvatarUrl ?: authorAvatarUrl,
        ),
        isOwn = isOwnProfile,
        onShopClick = onShopClick.takeIf { !checkIn.coffeeShopSlug.isNullOrBlank() },
        onPhotoClick = onPhotoClick,
        modifier = Modifier.testTag("profile-check-in:${checkIn.id}"),
        actions = {
            if (!isOwnProfile) com.coffeepeek.admin.ui.component.CheckInActions(
                onEditClick = null,
                onReportClick = { com.coffeepeek.admin.ui.Navigator.navigate(com.coffeepeek.admin.ui.Navigator.Screen.ReportCheckIn(checkIn.id)) },
                author = author,
            )
        },
    )
}

private fun PublicProfileCheckIn.legacyPresentation(): CheckIn {
    val isHome = venue == "Дома"
    return CheckIn(
        id = id,
        shopId = coffeeShopSlug.orEmpty(),
        shopName = venue,
        note = note,
        createdAt = occurredAt,
        photoUrls = photoUrls,
        customDrinkName = details.takeUnless { isHome },
        isHome = isHome,
        homePreparation = if (isHome) FeedHomePreparation(
            preparedOn = occurredAt.take(10), brewMethodCode = null, beanName = title,
            doseGrams = null, brewDurationSeconds = null, tasteScore = rating?.toInt(),
        ) else null,
    )
}

@Composable
internal fun ProfileEmptyCard(text: String, icon: ImageVector) {
    ProfileCard {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(23.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProfileCard(modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        content = content,
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ProfileHeaderLightPreview() = ProfileHeaderPreview(false)

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ProfileHeaderDarkPreview() = ProfileHeaderPreview(true)

@Composable
private fun ProfileHeaderPreview(dark: Boolean) = com.coffeepeek.core.designsystem.theme.CoffeePeekTheme(darkTheme = dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(16.dp)) {
            PublicProfileHeader("goloologo", null, "Варю, пью, фоткаю…", onEditProfile = {},
                stats = PublicProfileStats(24, 6, 9, 3), onAvatarClick = {})
        }
    }
}
