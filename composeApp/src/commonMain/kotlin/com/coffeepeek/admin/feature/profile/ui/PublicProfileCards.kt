package com.coffeepeek.admin.feature.profile.ui

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
import com.coffeepeek.admin.feature.profile.domain.PublicProfileStats
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
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
    onAvatarClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(92.dp)) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CpColor.GoldWarm, CpColor.Primary)))
                    .then(if (!avatarUrl.isNullOrBlank()) Modifier.clickable(role = Role.Button, onClickLabel = "Открыть аватар", onClick = onAvatarClick) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    nickname.trimStart('@').take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.headlineLarge,
                    color = CpColor.DarkTextOnPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!avatarUrl.isNullOrBlank()) CpImage(
                    data = avatarUrl, modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop, contentDescription = "Аватар @$nickname".replace("@@", "@"),
                )
            }
            if (onEditProfile != null) FilledIconButton(
                onClick = onEditProfile,
                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(CpIcons.Edit, contentDescription = "Редактировать профиль", modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "@${nickname.trimStart('@')}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onFollow != null) OutlinedButton(
                    onClick = onFollow,
                    enabled = followEnabled,
                    modifier = Modifier.semantics {
                        contentDescription = if (isFollowing) "Отписаться от @${nickname.trimStart('@')}" else "Подписаться на @${nickname.trimStart('@')}"
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(if (isFollowing) "Вы подписаны" else "Подписаться", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!about.isNullOrBlank()) Text(
                about, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PublicProfileStatsCard(stats: PublicProfileStats, onFollowersClick: () -> Unit = {}) {
    ProfileCard {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(
                stats.checkIns to "Чекины", stats.coffeeShops to "Кофейни",
                stats.roasters to "Обжарщики", stats.followers to "Подписчики",
            ).forEachIndexed { index, (count, label) ->
                if (index > 0) Box(Modifier.width(1.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(
                    Modifier.weight(1f).then(
                        if (index == 3) Modifier.clickable(role = Role.Button, onClickLabel = "Открыть подписчиков", onClick = onFollowersClick)
                        else Modifier,
                    ).padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PublicProfileCheckInCard(
    checkIn: PublicProfileCheckIn,
    onShopClick: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
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
