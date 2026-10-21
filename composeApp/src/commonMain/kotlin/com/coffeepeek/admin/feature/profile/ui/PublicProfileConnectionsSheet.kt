package com.coffeepeek.admin.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage

@Composable
internal fun PublicProfileConnectionsSheet(
    state: PublicProfileUiState,
    onDismiss: () -> Unit,
    onSelectKind: (PublicProfileConnectionKind) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onUserClick: (PublicProfileConnection) -> Unit,
) {
    val kind = state.connectionKind ?: return
    val kinds = listOf(PublicProfileConnectionKind.Followers, PublicProfileConnectionKind.Following)
    val listState = remember(kind) { LazyListState() }
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss, maxSheetHeightFraction = 0.9f) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(kind.label(), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDismiss) { Icon(CpIcons.Close, contentDescription = "Закрыть") }
        }
        TabRow(selectedTabIndex = kinds.indexOf(kind), containerColor = MaterialTheme.colorScheme.surface) {
            kinds.forEach { tab ->
                Tab(selected = tab == kind, onClick = { onSelectKind(tab) }, text = { Text(tab.label()) })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            items(state.connections, key = { it.slug }) { user ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .clickable(enabled = user.slug.isNotBlank(), role = Role.Button, onClickLabel = "Открыть профиль", onClick = { onUserClick(user) })
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(user.userName.trimStart('@').take(1).uppercase().ifBlank { "?" }, style = MaterialTheme.typography.titleMedium)
                        user.avatarUrl?.takeIf(String::isNotBlank)?.let { CpImage(it, modifier = Modifier.fillMaxSize()) }
                    }
                    Text(
                        "@${user.userName.trimStart('@')}", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Icon(CpIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            when {
                state.isLoadingConnections || state.isLoadingMoreConnections -> item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                }
                state.connectionsError != null -> item {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Не удалось загрузить ${if (kind == PublicProfileConnectionKind.Followers) "подписчиков" else "подписки"}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onRetry) { Text("Повторить") }
                    }
                }
                state.connections.isEmpty() -> item {
                    ProfileEmptyCard(
                        if (kind == PublicProfileConnectionKind.Followers) "Пока нет подписчиков" else "Пока нет подписок",
                        CpIcons.Community,
                    )
                }
                state.hasMoreConnections -> item {
                    TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text("Загрузить ещё") }
                }
            }
        }
    }
}

private fun PublicProfileConnectionKind.label(): String = when (this) {
    PublicProfileConnectionKind.Followers -> "Подписчики"
    PublicProfileConnectionKind.Following -> "Подписки"
}
