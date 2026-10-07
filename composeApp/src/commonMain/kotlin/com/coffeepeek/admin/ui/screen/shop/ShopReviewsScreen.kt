package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.GuestAuthCard
import com.coffeepeek.admin.ui.component.ReviewDisplayCard
import org.koin.core.parameter.parametersOf

@Composable
fun ShopReviewsScreen(shopId: String) {
    val vm: ShopDetailViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.uiState.collectAsState()
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    preview?.let { (urls, index) ->
        FullScreenImageDialog(imageUrls = urls, initialIndex = index, onDismiss = { preview = null })
    }
    Scaffold(topBar = { CpTopBar(title = "Чекины") }) { padding ->
        val details = state.details
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading && details == null -> CoffeePeekLoader()
                details == null && state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty())
                    Button(onClick = vm::load) { Text("Попробовать снова") }
                }
                details != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                ) {
                    item {
                        Text(details.shop.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (details.reviews.isNotEmpty()) item {
                        com.coffeepeek.admin.ui.component.ReviewRatingsOverview(details.reviews, details.shop.rating, details.shop.reviewCount)
                    }
                    state.actionMessage?.let { message -> item { Text(message) } }
                    if (details.reviews.isEmpty()) item { Text("Чекинов пока нет") }
                    itemsIndexed(details.reviews, key = { _, review -> review.id }) { index, review ->
                        val blurred = !state.isLoggedIn && index > 0
                        val own = state.currentUserId != null && review.userId == state.currentUserId
                        ReviewDisplayCard(
                            review = review,
                            modifier = if (blurred) Modifier.fillMaxWidth().blur(5.dp) else Modifier.fillMaxWidth(),
                            onPhotoClick = if (blurred) null else { urls, photoIndex -> preview = urls to photoIndex },
                            onHelpfulClick = if (blurred || own) null else ({ vm.toggleHelpful(review.id) }),
                            showHelpfulButton = !own,
                            fullVersion = true,
                            onReportClick = if (blurred) null else ({ Navigator.navigate(Navigator.Screen.ReportReview(review.id)) }),
                        )
                        if (!state.isLoggedIn && index == 0) GuestAuthCard(
                            onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
                            onRegister = { Navigator.navigate(Navigator.Screen.Register) },
                            modifier = Modifier.padding(top = CpDimens.spacing2),
                        )
                    }
                }
            }
        }
    }
}
