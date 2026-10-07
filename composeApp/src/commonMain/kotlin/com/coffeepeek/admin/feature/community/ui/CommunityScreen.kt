package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.ReviewDisplayCard
import com.coffeepeek.admin.ui.component.SavedDrinkBadge
import com.coffeepeek.domain.model.savedDrinkName
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating

@Composable
fun CommunityScreen() {
    var likedIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val clearance = LocalFloatingNavClearance.current

    preview?.let { (urls, index) ->
        FullScreenImageDialog(imageUrls = urls, initialIndex = index, onDismiss = { preview = null })
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    start = CpDimens.spacing4, end = CpDimens.spacing4,
                    bottom = clearance + CpDimens.buttonHeight + CpDimens.spacing4 * 2,
                ),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            ) {
                item {
                    Text(
                        "Лента", modifier = Modifier.fillMaxWidth().padding(top = CpDimens.spacing3),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(previewPosts, key = { it.visit.id }) { post ->
                    val liked = post.visit.id in likedIds
                    ReviewDisplayCard(
                        review = post.visit,
                        authorPhotoUrl = post.avatar,
                        dateLabel = post.timeLabel,
                        fullVersion = true,
                        showDrinkBadge = false,
                        onReportClick = { Navigator.navigate(Navigator.Screen.ReportReview(post.visit.id, isPreview = true)) },
                        onPhotoClick = { urls, index -> preview = urls to index },
                        footer = {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = CpDimens.spacing1),
                                horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CpImage(
                                    data = post.shopPhoto,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(CpDimens.radiusMd)),
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                                    Text(post.shopName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                                        Icon(CpIcons.Location, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(post.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        likedIds = if (liked) likedIds - post.visit.id else likedIds + post.visit.id
                                    },
                                    shape = RoundedCornerShape(CpDimens.buttonRadius),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2),
                                    modifier = Modifier.semantics {
                                        contentDescription = "Нравится чекин ${post.visit.username}"
                                        stateDescription = if (liked) "Отмечено" else "Не отмечено"
                                    },
                                ) {
                                    Icon(
                                        if (liked) CpIcons.FavoriteFilled else CpIcons.Favorite, null,
                                        Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "${post.likes + if (liked) 1 else 0}",
                                        Modifier.padding(start = CpDimens.spacing2),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                savedDrinkName(
                                    post.visit.drinkNameRu, post.visit.drinkNameEn,
                                    post.visit.customDrinkName, Locale.current.language,
                                )?.let { SavedDrinkBadge(it, showLabel = false) }
                            }
                        },
                    )
                }
            }
        }
        PlatformGlassIconButton(
            icon = GlassControlIcon.Add,
            contentDescription = "Добавить чекин",
            onClick = {},
            enabled = false,
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = CpDimens.spacing4, bottom = clearance + CpDimens.spacing4),
        ) { Icon(CpIcons.Add, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface) }
    }
}

// Preview content stays local until the feed API is connected.
private data class PreviewPost(
    val visit: Review,
    val avatar: String,
    val timeLabel: String,
    val shopName: String,
    val shopPhoto: String,
    val address: String,
    val likes: Int,
)

private fun photo(id: String) = "https://images.unsplash.com/$id?auto=format&fit=crop&w=800&q=85"

private val coffeePhoto = photo("photo-1509042239860-f550ce710b93")
private val interiorPhoto = photo("photo-1501339847302-ac426a4a7cbb")
private val espressoPhoto = photo("photo-1514432324607-a09d9b4aefdd")

private val previewPosts = listOf(
    PreviewPost(
        visit = Review(
            id = "preview-maria", username = "Maria", header = "",
            comment = "Идеальное утро с капучино на кокосовом 🥥\nАтмосферное место в самом центре города.",
            rating = ReviewRating(place = 4, service = 4, coffee = 5), createdAt = "",
            drinkNameRu = "Капучино", photoUrls = listOf(coffeePhoto, interiorPhoto, espressoPhoto),
        ),
        avatar = photo("photo-1494790108377-be9c29b29330"), timeLabel = "2 ч. назад",
        shopName = "Ranak", shopPhoto = interiorPhoto,
        address = "Ульяновская улица, 30 · 7,6 км от вас", likes = 13,
    ),
    PreviewPost(
        visit = Review(
            id = "preview-katya", username = "Катя", header = "",
            comment = "Лучший эспрессо в городе. Всегда стабильно",
            rating = ReviewRating(place = 4, service = 5, coffee = 5), createdAt = "",
            drinkNameRu = "Эспрессо",
        ),
        avatar = photo("photo-1534528741775-53994a69daeb"), timeLabel = "Вчера",
        shopName = "1801 кофе", shopPhoto = espressoPhoto,
        address = "Кальварийская улица, 21 · 3,1 км от вас", likes = 8,
    ),
    PreviewPost(
        visit = Review(
            id = "preview-ilya", username = "Илья", header = "",
            comment = "Заглянул за фильтром и остался читать. Спокойная музыка, хороший кофе и столик у окна.",
            rating = ReviewRating(place = 4, service = 4, coffee = 4), createdAt = "",
        ),
        avatar = photo("photo-1500648767791-00dcc994a43e"), timeLabel = "2 дня назад",
        shopName = "Кофейня у дома", shopPhoto = interiorPhoto,
        address = "Независимости, 43 · 1,2 км от вас", likes = 5,
    ),
)
