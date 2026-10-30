package com.coffeepeek.feature.profile.impl.ui.compose.component

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState

@Composable
internal fun PublicProfileConnectionsSheet(
    state: PublicProfileState,
    image: ProfileImage,
    onDismiss: () -> Unit,
    onSelectKind: (PublicProfileConnectionKind) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onUserClick: (PublicProfileConnection) -> Unit,
) {
    val kind = state.connectionKind ?: return
    val kinds = listOf(PublicProfileConnectionKind.Followers, PublicProfileConnectionKind.Following)
    val listState = remember(kind) { LazyListState() }
    val close = stringResource(Res.string.profile_close)
    CoffeePeekSwipeDismissModalBottomSheet(onDismissRequest = onDismiss, dismissDescription = close, maxSheetHeightFraction = 0.9f) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(kind.label(), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDismiss) { Icon(CoffeePeekIcons.Close, contentDescription = close) }
        }
        TabRow(selectedTabIndex = kinds.indexOf(kind), containerColor = MaterialTheme.colorScheme.surface) {
            kinds.forEach { tab -> Tab(selected = tab == kind, onClick = { onSelectKind(tab) }, text = { Text(tab.label()) }) }
        }
        val openProfile = stringResource(Res.string.profile_open_user)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            items(state.connections, key = { it.slug }) { user ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .clickable(enabled = user.slug.isNotBlank(), role = Role.Button, onClickLabel = openProfile, onClick = { onUserClick(user) })
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(user.userName.trimStart('@').take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.titleMedium)
                        user.avatarUrl?.takeIf(String::isNotBlank)?.let { image(it, null, Modifier.fillMaxSize()) }
                    }
                    Text(
                        "@${user.userName.trimStart('@')}", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            when {
                state.isLoadingConnections || state.isLoadingMoreConnections -> item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { ProfileSectionLoader(48.dp) }
                }
                state.connectionsError != null -> item {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(if (kind == PublicProfileConnectionKind.Followers) Res.string.profile_followers_failed else Res.string.profile_following_failed),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onRetry) { Text(stringResource(Res.string.profile_retry)) }
                    }
                }
                state.connections.isEmpty() -> item {
                    ProfileEmptyCard(
                        stringResource(if (kind == PublicProfileConnectionKind.Followers) Res.string.profile_followers_empty else Res.string.profile_following_empty),
                        CoffeePeekIcons.User,
                    )
                }
                state.hasMoreConnections -> item {
                    TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.profile_load_more)) }
                }
            }
        }
    }
}

@Composable
private fun PublicProfileConnectionKind.label(): String = stringResource(
    if (this == PublicProfileConnectionKind.Followers) Res.string.profile_followers else Res.string.profile_following_title,
)
