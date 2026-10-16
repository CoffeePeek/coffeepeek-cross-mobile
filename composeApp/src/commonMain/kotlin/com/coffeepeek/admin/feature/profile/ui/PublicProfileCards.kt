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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.admin.feature.profile.domain.PublicProfileStats
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.component.rememberReviewDisplayDate
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import kotlin.math.roundToInt

@Composable
internal fun PublicProfileHeader(
    nickname: String,
    avatarUrl: String?,
    about: String?,
    isFollowing: Boolean = false,
    followEnabled: Boolean = true,
    onFollow: (() -> Unit)? = null,
    onAvatarClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(92.dp).clip(CircleShape)
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
    compact: Boolean = true,
) {
    ProfileCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        Modifier.then(if (!checkIn.coffeeShopSlug.isNullOrBlank()) Modifier.clickable(role = Role.Button, onClickLabel = "Открыть кофейню", onClick = onShopClick) else Modifier),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(CpIcons.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
                        Text(
                            checkIn.venue, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (checkIn.occurredAt.isNotBlank()) Text(
                        rememberReviewDisplayDate(checkIn.occurredAt), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(checkIn.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    checkIn.details?.takeIf(String::isNotBlank)?.let {
                        val parts = it.split(" · ")
                        val details = (listOf(parts.first().methodLabel()) + parts.drop(1)).joinToString(" · ")
                        Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    checkIn.rating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(CpIcons.StarFilled, contentDescription = "Оценка", tint = CpColor.Primary, modifier = Modifier.size(17.dp))
                            Text(((rating * 10).roundToInt() / 10.0).toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                checkIn.photoUrls.firstOrNull()?.let { url ->
                    Box(Modifier.size(92.dp).clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClickLabel = "Открыть фото", onClick = { onPhotoClick(checkIn.photoUrls, 0) })) {
                        CpImage(data = url, modifier = Modifier.fillMaxSize(), contentDescription = "Фото чекина: ${checkIn.title}")
                        if (checkIn.photoUrls.size > 1) Surface(
                            Modifier.align(Alignment.BottomEnd).padding(5.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("+${checkIn.photoUrls.size - 1}", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (checkIn.note.isNotBlank()) Text(
                checkIn.note, style = MaterialTheme.typography.bodyMedium,
                maxLines = if (compact) 3 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
