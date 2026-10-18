package com.coffeepeek.admin.feature.authorblocking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import org.koin.compose.koinInject

@Composable
internal fun BlockedAuthorsScreen(vm: AuthorBlockingPresenter = koinInject()) {
    val state by vm.blocks.collectAsState()
    val prompt by vm.prompt.collectAsState()
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(vm) { vm.refresh() }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        CpTopBar(title = "Блокировки")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = CpDimens.settingsPagePadding, vertical = CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            item {
                BasicText("Заблокированные авторы", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall.copy(color = colors.onBackground))
                Spacer(Modifier.height(CpDimens.spacing2))
                BasicText("Вы не видите публикации этих авторов. Разблокировка не восстанавливает подписки.", style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant))
                Spacer(Modifier.height(CpDimens.spacing2))
            }
            when {
                !state.signedIn -> item {
                    GuestAuthCard(onLogin = { Navigator.navigate(Navigator.Screen.Auth) }, onRegister = { Navigator.navigate(Navigator.Screen.Register) })
                }
                else -> {
                    if (state.loading) item {
                        Box(Modifier.fillMaxWidth().padding(CpDimens.spacing8), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                    }
                    state.error?.let { error -> item {
                        BlockedAuthorsCard {
                            BasicText("Не удалось загрузить список", style = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface))
                            BasicText(error, style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant))
                            CpTextAction("Повторить", vm::refresh, Modifier.align(Alignment.Start), emphasized = true)
                        }
                    } }
                    if (state.ready && !state.loading && state.error == null && state.items.isEmpty()) item {
                        CpEmptyState("Нет заблокированных авторов", "Когда вы заблокируете автора, он появится здесь.", CpIcons.VisibilityOff, Modifier.padding(top = CpDimens.spacing6))
                    }
                    items(state.items, key = { it.id }) { author ->
                        val shape = RoundedCornerShape(CpDimens.cardRadius)
                        FlowRow(Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape)
                            .padding(CpDimens.spacing3),
                            itemVerticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                            Row(Modifier.weight(1f).widthIn(min = 140.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                ReviewAvatar(author.name, author.avatarUrl)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                                    BasicText(author.name, style = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    author.slug?.let { BasicText("@$it", style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                            CpTextAction("Разблокировать", enabled = prompt?.working != true,
                                onClick = { vm.requestUnblock(AuthorReference(author.id, author.slug, author.name, author.avatarUrl)) })
                        }
                    }
                    item { Spacer(Modifier.navigationBarsPadding().height(CpDimens.spacing4)) }
                }
            }
        }
    }
}

@Composable
private fun BlockedAuthorsCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CpDimens.cardRadius)
    Column(Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape).padding(CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2), content = content)
}
