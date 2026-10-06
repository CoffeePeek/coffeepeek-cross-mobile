package com.coffeepeek.admin.ui.screen.contributions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.ReviewDisplayCard
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconColors
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.review.EditReviewBottomSheet
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.ShopChangeSection
import org.koin.core.parameter.parametersOf

@Composable
fun MyContributionsScreen(kind: ContributionKind) {
    val vm: MyContributionsViewModel = platformViewModel(
        key = "contributions-$kind",
        parameters = { parametersOf(kind) },
    )
    val state by vm.state.collectAsState()
    var editingReviewId by remember { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var photoPreview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    photoPreview?.let { (urls, index) ->
        FullScreenImageDialog(
            imageUrls = urls,
            initialIndex = index,
            onDismiss = { photoPreview = null },
        )
    }

    editingReviewId?.let { reviewId ->
        EditReviewBottomSheet(
            reviewId = reviewId,
            placeName = null,
            onDismiss = { editingReviewId = null },
            onSaved = {
                editingReviewId = null
                vm.refresh()
            },
        )
    }

    Scaffold(
        topBar = { CpTopBar(kind.title) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val tabs = state.visibleTabs
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CoffeePeekLoader()
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Ошибка", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(CpDimens.spacing3))
                    Button(
                        onClick = vm::refresh,
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                    ) { Text("Попробовать снова") }
                }
            }
            tabs.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(kind.emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                val selected = tabs.firstOrNull { it.name == selectedName } ?: tabs.first()
                CapsuleSegmentedControl(
                    options = tabs,
                    selected = selected,
                    label = { status -> "${status.tabTitle()} · ${state.tabs[status]?.totalCount ?: 0}" },
                    onSelected = { selectedName = it.name },
                    modifier = Modifier.padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
                )
                key(selected) {
                    ContributionList(
                        kind = kind,
                        status = selected,
                        tab = state.tabs[selected] ?: ContributionTab(),
                        onLoadMore = { vm.loadMore(selected) },
                        onEditReview = { editingReviewId = it },
                        onPhotoClick = { urls, index -> photoPreview = urls to index },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributionList(
    kind: ContributionKind,
    status: ModerationStatus,
    tab: ContributionTab,
    onLoadMore: () -> Unit,
    onEditReview: (String) -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(tab) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= tab.items.size - 3 && tab.hasMore && !tab.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        items(tab.items, key = { it.id }) { item ->
            if (item.review != null) {
                Column {
                    ReviewDisplayCard(
                        review = item.review,
                        onEditClick = if (item.editable) ({ onEditReview(item.review.id) }) else null,
                        onPhotoClick = onPhotoClick,
                        showHelpfulButton = false,
                    )
                    item.rejectedReason?.let { RejectedReason(it) }
                }
            } else {
                ContributionCard(item = item, kind = kind, status = status)
            }
        }
    }
}

@Composable
private fun ContributionCard(
    item: ContributionItem,
    kind: ContributionKind,
    status: ModerationStatus,
) {
    val target = item.target
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(CpDimens.radiusLg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (target != null) Modifier.clickable { Navigator.navigate(target) } else Modifier),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(CpDimens.spacing4),
                verticalAlignment = Alignment.Top,
            ) {
                val visual = contributionVisual(kind, item.changeSection)
                SettingsIconBadge(icon = visual.icon, colors = visual.colors)
                Spacer(Modifier.width(CpDimens.spacing3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!item.subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(CpDimens.spacing1))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (kind == ContributionKind.Roasters) 3 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!item.meta.isNullOrBlank()) {
                        Spacer(Modifier.height(CpDimens.spacing2))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                        ) {
                            Icon(
                                imageVector = if (kind == ContributionKind.Changes) CpIcons.Time else CpIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = item.meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(CpDimens.spacing2))
                ModerationStatusBadge(status)
            }
            item.rejectedReason?.let { RejectedReason(it) }
            if (target != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (kind == ContributionKind.Changes) "Посмотреть заявку" else "Открыть в каталоге",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = CpIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RejectedReason(reason: String) {
    if (reason.isBlank()) return
    Row(
        modifier = Modifier
            .padding(horizontal = CpDimens.spacing4)
            .padding(bottom = CpDimens.spacing4)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
            .padding(CpDimens.spacing3),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Icon(
            imageVector = CpIcons.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                text = "Почему отклонено",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ModerationStatusBadge(status: ModerationStatus) {
    val color = status.color()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = CpDimens.spacing2, vertical = CpDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(color),
        )
        Text(
            text = status.shortTitle(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

private data class ContributionVisual(
    val icon: ImageVector,
    val colors: SettingsIconColors,
)

private fun contributionVisual(kind: ContributionKind, section: ShopChangeSection?) = when (kind) {
    ContributionKind.Shops -> ContributionVisual(CpIcons.Coffee, SettingsIconPalette.Gold)
    ContributionKind.Roasters -> ContributionVisual(CpIcons.CoffeeBean, SettingsIconPalette.Rose)
    ContributionKind.Changes -> when (section) {
        ShopChangeSection.Photos -> ContributionVisual(CpIcons.Photo, SettingsIconPalette.Sky)
        ShopChangeSection.Contacts -> ContributionVisual(CpIcons.Phone, SettingsIconPalette.Mint)
        ShopChangeSection.Description -> ContributionVisual(CpIcons.NoteEdit, SettingsIconPalette.Lavender)
        ShopChangeSection.Tags -> ContributionVisual(CpIcons.Sparkle, SettingsIconPalette.Gold)
        ShopChangeSection.Roasters -> ContributionVisual(CpIcons.CoffeeBean, SettingsIconPalette.Rose)
        ShopChangeSection.Equipment -> ContributionVisual(CpIcons.Settings, SettingsIconPalette.Cyan)
        ShopChangeSection.Menu -> ContributionVisual(CpIcons.Menu, SettingsIconPalette.Emerald)
        ShopChangeSection.BrewMethods -> ContributionVisual(CpIcons.Coffee, SettingsIconPalette.Aqua)
        null -> ContributionVisual(CpIcons.Edit, SettingsIconPalette.Violet)
    }
    ContributionKind.Reviews -> ContributionVisual(CpIcons.Review, SettingsIconPalette.Lavender)
}

private fun ModerationStatus.color(): Color = when (this) {
    ModerationStatus.Approved -> CpColor.Success
    ModerationStatus.Pending -> CpColor.GoldWarmHover
    ModerationStatus.Rejected -> CpColor.Error
}

private fun ModerationStatus.shortTitle(): String = when (this) {
    ModerationStatus.Approved -> "Готово"
    ModerationStatus.Pending -> "Проверяется"
    ModerationStatus.Rejected -> "Отклонено"
}

private fun ModerationStatus.tabTitle() = when (this) {
    ModerationStatus.Approved -> "Опубликовано"
    ModerationStatus.Pending -> "На модерации"
    ModerationStatus.Rejected -> "Отклонено"
}
