package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.coffeepeek.admin.ui.component.liquidGlass
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.SwipeablePhotoStack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.admin.utils.formatOneDecimal
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_with_book
import coffeepeek.composeapp.generated.resources.shop_report_issue
import coffeepeek.composeapp.generated.resources.shop_report_prompt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.CheckInDisplayCard
import com.coffeepeek.admin.ui.component.GuestAuthCard
import com.coffeepeek.admin.ui.component.ReviewDisplayCard
import com.coffeepeek.admin.utils.currentLocalDayOfWeek
import com.coffeepeek.admin.utils.currentLocalMinuteOfDay
import com.coffeepeek.admin.ui.component.PriceBynRow
import com.coffeepeek.admin.ui.component.PriceBynIcon
import com.coffeepeek.admin.ui.component.priceRangeLevel
import com.coffeepeek.admin.ui.component.priceLevelValue
import com.coffeepeek.admin.ui.component.shopTagIcon
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.FavoriteButton
import com.coffeepeek.admin.ui.screen.review.CreateReviewBottomSheet
import com.coffeepeek.admin.ui.screen.review.EditReviewBottomSheet
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ShopContact
import com.coffeepeek.domain.model.ShopMenu
import com.coffeepeek.domain.model.ShopMenuItem
import com.coffeepeek.domain.model.ShopSchedule
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

private val GuestReviewPeekWidth = 56.dp
private val ReviewCardMaxWidth = 320.dp
private const val FeaturePreviewCount = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailScreen(shopId: String) {
    val vm: ShopDetailViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.uiState.collectAsState()
    val userLocation = rememberPermittedUserLocation()
    val snackbarHostState = remember { SnackbarHostState() }
    // Photos open as a swipeable set: (urls, startIndex).
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.clearActionMessage()
        }
    }

    preview?.let { (urls, index) ->
        FullScreenImageDialog(imageUrls = urls, initialIndex = index, onDismiss = { preview = null })
    }

    if (state.showCheckInSheet) {
        state.checkInDraft?.let { draft ->
            CheckInBottomSheet(
                drinks = state.drinks,
                drinksError = state.drinksError,
                onRetryDrinks = vm::loadDrinks,
                draft = draft,
                isLoading = state.isCheckInLoading,
                onDismiss = vm::dismissCheckInSheet,
                onDraftChange = vm::updateCheckInDraft,
                onSubmit = vm::checkIn,
                placeName = state.details?.shop?.title,
            )
        }
    }

    if (state.showReviewSheet) {
        val reviewId = state.editingReviewId
        if (reviewId == null) {
            CreateReviewBottomSheet(
                shopId = shopId,
                placeName = state.details?.shop?.title,
                onDismiss = vm::dismissReviewSheet,
            )
        } else {
            EditReviewBottomSheet(
                reviewId = reviewId,
                placeName = state.details?.shop?.title,
                onDismiss = vm::dismissReviewSheet,
            )
        }
    }

    val details = state.details
    val distance = formatDistance(distanceToShopMeters(userLocation, details?.location))
    val floatingActionsClearance = 72.dp
    val hazeState = rememberHazeState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.hazeSource(hazeState),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = floatingActionsClearance),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CoffeePeekLoader()
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Ошибка",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(CpDimens.spacing3))
                            Button(
                                onClick = vm::load,
                                modifier = Modifier.height(CpDimens.buttonHeight),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) { Text("Повторить") }
                        }
                    }
                }
                details != null -> {
                    ShopDetailContent(
                        details = details,
                        distance = distance,
                        isLoggedIn = state.isLoggedIn,
                        currentUserId = state.currentUserId,
                        modifier = Modifier.padding(padding),
                        bottomContentPadding = floatingActionsClearance,
                        onOpenOnMap = vm::openOnMap,
                        onReportIssue = vm::openReportIssue,
                        favoriteRoasterIds = state.favoriteRoasterIds,
                        savingRoasterFavoriteIds = state.savingRoasterFavoriteIds,
                        onToggleRoasterFavorite = vm::toggleRoasterFavorite,
                        onCopyPhone = vm::copyPhone,
                        onOpenPhotos = { urls, index -> preview = urls to index },
                        onReviewPhotoClick = { urls, index -> preview = urls to index },
                        onReviewHelpfulClick = vm::toggleHelpful,
                    )
                }
            }
        }

        if (details != null) {
            HeroTopActions(
                hazeState = hazeState,
                onBack = Navigator::popBack,
                isFavorite = details.shop.isFavorite,
                isFavoriteLoading = state.isFavoriteLoading,
                onToggleFavorite = vm::toggleFavorite,
                onShare = vm::shareShop,
                onSuggestChange = vm::openSuggestChange,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
            )

            ShopDetailBottomBar(
                isCheckInLoading = state.isCheckInLoading,
                canOpenRoute = details.location?.latitude != null &&
                    details.location?.longitude != null,
                onRoute = vm::openRoute,
                onReview = vm::openReviewAction,
                onCheckIn = vm::openCheckInSheet,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopDetailContent(
    details: CoffeeShopDetails,
    distance: String?,
    isLoggedIn: Boolean,
    currentUserId: String? = null,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = CpDimens.spacing4,
    onOpenOnMap: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    favoriteRoasterIds: Set<String> = emptySet(),
    savingRoasterFavoriteIds: Set<String> = emptySet(),
    onToggleRoasterFavorite: (CatalogItem) -> Unit = {},
    onCopyPhone: (String) -> Unit = {},
    onOpenPhotos: (List<String>, Int) -> Unit = { _, _ -> },
    onReviewPhotoClick: (List<String>, Int) -> Unit = { _, _ -> },
    onReviewHelpfulClick: (String) -> Unit = {},
) {
    val shop = details.shop
    val photos = details.photos.filter { it.isNotBlank() }.ifEmpty {
        listOfNotNull(shop.photoUrl?.takeIf { it.isNotBlank() })
    }
    // Same order as photos; if they diverge, the viewer just gets the hero-sized ones.
    val fullscreenPhotos = details.fullscreenPhotos.filter { it.isNotBlank() }
        .takeIf { it.size == photos.size } ?: photos

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding + CpDimens.spacing4),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                ShopHeroImage(
                    photos = photos,
                    title = shop.title,
                    address = details.location?.address ?: shop.address,
                    distance = distance,
                    shopType = shop.type,
                    canOpenMap = details.location?.latitude != null &&
                        details.location?.longitude != null,
                    onOpenOnMap = onOpenOnMap,
                    onPhotoClick = { url -> onOpenPhotos(fullscreenPhotos, photos.indexOf(url).coerceAtLeast(0)) },
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CpDimens.spacing4)
                    .padding(top = CpDimens.spacing5, bottom = CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
            ) {
                ShopStatsRow(
                    rating = shop.rating,
                    reviewCount = shop.reviewCount,
                    isOpen = shop.isOpen,
                    priceRange = shop.priceRange,
                    schedules = details.schedules,
                )
            }
        }

        details.description?.trim()?.takeIf { it.isNotBlank() }?.let { description ->
            item {
                DescriptionSection(description = description)
            }
        }

        if (
            details.coffeeBeans.isNotEmpty() ||
            details.roasters.isNotEmpty() ||
            details.equipment.isNotEmpty()
        ) {
            item {
                CoffeeDetailsSection(
                    coffeeBeans = details.coffeeBeans,
                    roasters = details.roasters,
                    equipment = details.equipment,
                    favoriteRoasterIds = favoriteRoasterIds,
                    savingRoasterFavoriteIds = savingRoasterFavoriteIds,
                    onToggleRoasterFavorite = onToggleRoasterFavorite,
                    onRoasterClick = {
                        Navigator.navigate(Navigator.Screen.RoasterDetail(it))
                    },
                )
            }
        }

        if (details.schedules.isNotEmpty()) {
            item {
                CollapsibleScheduleSection(schedules = details.schedules, isOpen = shop.isOpen)
            }
        }

        details.menu?.takeIf { menu ->
            groupedPresentItems(menu.items).isNotEmpty() || menu.photos.isNotEmpty()
        }?.let { menu ->
            item {
                MenuSection(
                    menu = menu,
                    onPhotoClick = { index -> onOpenPhotos(menu.photos.map { it.fullUrl }, index) },
                )
            }
        }

        details.contact?.let { contact ->
            if (contact.hasAny()) {
                item {
                    ContactsSection(
                        contact = contact,
                        onCopyPhone = onCopyPhone,
                    )
                }
            }
        }

        if (details.userCheckIns.isNotEmpty()) {
            item {
                CheckInsSection(
                    checkIns = details.userCheckIns,
                    onPhotoClick = onReviewPhotoClick,
                )
            }
        }

        val features = shopFeatureItems(details)
        if (features.isNotEmpty()) {
            item {
                ShopFeaturesSection(features = features)
            }
        }

        item {
            ReviewsSection(
                reviews = details.reviews,
                overallRating = shop.rating,
                reviewCount = shop.reviewCount,
                shopId = shop.id,
                shopTitle = shop.title,
                isLoggedIn = isLoggedIn,
                currentUserId = currentUserId,
                onReviewPhotoClick = onReviewPhotoClick,
                onReviewHelpfulClick = onReviewHelpfulClick,
            )
        }

        item {
            ReportIssueSection(onReportIssue = onReportIssue)
        }

        item { Spacer(Modifier.height(CpDimens.spacing6)) }
    }
}

@Composable
private fun ShopHeroImage(
    photos: List<String>,
    title: String,
    address: String?,
    distance: String?,
    shopType: String,
    canOpenMap: Boolean,
    onOpenOnMap: () -> Unit,
    onPhotoClick: (String) -> Unit,
) {
    var currentPhotoIndex by remember(photos) { mutableStateOf(0) }
    val heroShape = RoundedCornerShape(
        bottomStart = CpDimens.radius3xl,
        bottomEnd = CpDimens.radius3xl,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(heroShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (photos.size <= 1) {
            val coverUrl = photos.firstOrNull()
            if (!coverUrl.isNullOrBlank()) {
                CoffeeShopImage(
                    imageUrl = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    placeholderLabelSize = 24.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPhotoClick(coverUrl) },
                )
            } else {
                CoffeeShopPlaceholderImage(
                    labelSize = 24.sp,
                    contentDescription = "Фото $title отсутствует",
                )
            }
        } else {
            PhotoGallery(
                photos = photos,
                title = title,
                onPhotoClick = onPhotoClick,
                onPageChanged = { currentPhotoIndex = it },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    )
                )
        )

        HeroShopDetails(
            title = title,
            address = address,
            distance = distance,
            canOpenMap = canOpenMap,
            onOpenOnMap = onOpenOnMap,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = CpDimens.spacing4, end = 116.dp, bottom = CpDimens.spacing4),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = CpDimens.spacing4, bottom = CpDimens.spacing4),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            ShopTypeBadge(shopType = shopType)
            if (photos.isNotEmpty()) {
                PhotoCounter(
                    current = currentPhotoIndex + 1,
                    total = photos.size,
                )
            }
        }
    }
}

@Composable
private fun HeroTopActions(
    hazeState: HazeState,
    onBack: () -> Unit,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onSuggestChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(CpDimens.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        HeroIconButton(
            icon = GlassControlIcon.Back,
            hazeState = hazeState,
            onClick = onBack,
            enabled = true,
            isLoading = false,
            contentDescription = "Назад",
        ) {
            Icon(
                imageVector = CpIcons.Back,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        HeaderActionButtons(
            hazeState = hazeState,
            isFavorite = isFavorite,
            isFavoriteLoading = isFavoriteLoading,
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onSuggestChange = onSuggestChange,
        )
    }
}

@Composable
private fun HeroShopDetails(
    title: String,
    address: String?,
    distance: String?,
    canOpenMap: Boolean,
    onOpenOnMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val addressLabel = address?.takeIf { it.isNotBlank() }
            ?: if (canOpenMap) "Показать на карте" else null
        addressLabel?.let { label ->
            Row(
                modifier = Modifier.clickable(enabled = canOpenMap, onClick = onOpenOnMap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CpIcons.Location,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(CpDimens.spacing1))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        distance?.let { label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = CpIcons.Distance,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(CpDimens.spacing1))
                Text(
                    text = "$label от вас",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ReportIssueSection(onReportIssue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.shop_report_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onReportIssue) {
            Text(stringResource(Res.string.shop_report_issue))
        }
    }
}

@Composable
private fun ShopTypeBadge(shopType: String) {
    val label = when (shopType) {
        CoffeeShopType.SPECIALTY -> "SPECIALTY"
        CoffeeShopType.CAFE -> "КАФЕ"
        else -> "КОФЕЙНЯ"
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.48f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CpColor.Primary),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CpDimens.spacing2, vertical = CpDimens.spacing1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        ) {
            Icon(
                imageVector = CpIcons.Coffee,
                contentDescription = null,
                tint = CpColor.Primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun PhotoCounter(current: Int, total: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.48f),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = "$current / $total",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing1),
        )
    }
}

@Composable
private fun HeaderActionButtons(
    hazeState: HazeState,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onSuggestChange: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeroIconButton(
            icon = GlassControlIcon.Edit,
            hazeState = hazeState,
            onClick = onSuggestChange,
            enabled = true,
            isLoading = false,
            contentDescription = "Предложить правку",
        ) {
            Icon(
                imageVector = CpIcons.NoteEdit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        HeroIconButton(
            icon = if (isFavorite) GlassControlIcon.FavoriteFilled else GlassControlIcon.Favorite,
            hazeState = hazeState,
            onClick = onToggleFavorite,
            enabled = !isFavoriteLoading,
            isLoading = isFavoriteLoading,
            contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
        ) {
            Icon(
                imageVector = if (isFavorite) CpIcons.FavoriteFilled else CpIcons.Favorite,
                contentDescription = null,
                tint = if (isFavorite) CpColor.Error else MaterialTheme.colorScheme.onSurface,
            )
        }
        HeroIconButton(
            icon = GlassControlIcon.Share,
            hazeState = hazeState,
            onClick = onShare,
            enabled = true,
            isLoading = false,
            contentDescription = "Поделиться",
        ) {
            Icon(
                imageVector = CpIcons.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ShopStatsRow(
    rating: Double?,
    reviewCount: Int,
    isOpen: Boolean,
    priceRange: String?,
    schedules: List<ShopSchedule>,
) {
    val currentDay = currentLocalDayOfWeek()
    val todaySchedule = remember(schedules, currentDay) {
        schedules.firstOrNull { it.dayOfWeek == currentDay }
    }
    val closingTime = todaySchedule
        ?.takeUnless { it.isClosed }
        ?.intervals
        ?.lastOrNull()
        ?.closeTime
        ?.let(::formatTime)
        ?.takeIf { it.isNotBlank() }
    val priceLevel = priceRangeLevel(priceRange)

    OutlinedContentCard(contentPadding = PaddingValues(vertical = CpDimens.spacing4)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCard(
                modifier = Modifier.weight(1.15f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = CpIcons.StarFilled,
                        contentDescription = null,
                        tint = CpColor.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = rating?.takeIf { it > 0.0 }?.let(::formatOneDecimal) ?: "Нет оценок",
                        style = if (rating != null && rating > 0.0) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = reviewCountLabel(reviewCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline)
            StatCard(modifier = Modifier.weight(1.1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOpen) CpColor.Success else CpColor.Error),
                    )
                    Text(
                        text = if (isOpen) "Открыто" else "Закрыто",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isOpen) CpColor.Success else CpColor.Error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = when {
                        isOpen && closingTime != null -> "до $closingTime"
                        isOpen -> "Сегодня"
                        else -> nextShopOpeningLabel(schedules, currentDay, currentLocalMinuteOfDay())
                            ?: if (todaySchedule?.isClosed == true) "Сегодня выходной" else "Расписание не указано"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline)
            StatCard(modifier = Modifier.weight(1f)) {
                if (priceLevel != null) {
                    PriceBynRow(level = priceLevel, iconSize = 18.dp)
                    Text(
                        text = priceLevelValue(priceLevel).orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Средний чек",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = CpDimens.spacing3),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        content = content,
    )
}

private fun reviewCountLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val word = when {
        mod100 in 11..14 -> "отзывов"
        mod10 == 1 -> "отзыв"
        mod10 in 2..4 -> "отзыва"
        else -> "отзывов"
    }
    return "$count $word"
}


private data class ShopFeatureItem(
    val name: String,
    val slug: String?,
    val isBrewMethod: Boolean,
)

private fun shopFeatureItems(details: CoffeeShopDetails): List<ShopFeatureItem> {
    val brewMethods = details.brewMethodItems
        .filter { it.name.isNotBlank() }
        .map { item ->
            ShopFeatureItem(name = item.name, slug = item.slug, isBrewMethod = true)
        }
        .ifEmpty {
            details.brewMethods.map { name ->
                ShopFeatureItem(name = name, slug = null, isBrewMethod = true)
            }
        }
    val tags = details.tagItems
        .filter { it.name.isNotBlank() }
        .map { item ->
            ShopFeatureItem(name = item.name, slug = item.slug, isBrewMethod = false)
        }
        .ifEmpty {
            details.shop.tags.map { name ->
                ShopFeatureItem(name = name, slug = null, isBrewMethod = false)
            }
        }

    return (brewMethods + tags).distinctBy { it.name.trim().lowercase() }
}

@Composable
private fun HeroIconButton(
    icon: GlassControlIcon,
    hazeState: HazeState,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    PlatformGlassIconButton(
        icon = icon,
        onClick = onClick,
        enabled = enabled,
        isLoading = isLoading,
        contentDescription = contentDescription,
        hazeState = hazeState,
    ) {
        if (isLoading) {
            CoffeePeekLoader(
                size = 18.dp,
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            content()
        }
    }
}

@Composable
private fun CollapsibleScheduleSection(schedules: List<ShopSchedule>, isOpen: Boolean) {
    var expanded by remember { mutableStateOf(true) }
    val currentDay = currentLocalDayOfWeek()
    val orderedSchedules = remember(schedules) {
        schedules.sortedBy { schedule ->
            if (schedule.dayOfWeek == 0) 7 else schedule.dayOfWeek
        }
    }
    val todaySchedule = schedules.firstOrNull { it.dayOfWeek == currentDay }
    val closingTime = todaySchedule?.takeUnless { it.isClosed }
        ?.intervals?.lastOrNull()?.closeTime?.let(::formatTime)
        ?.takeIf { it.isNotBlank() }
    val status = when {
        isOpen && closingTime != null -> "Открыто до $closingTime"
        isOpen -> "Открыто"
        else -> nextShopOpeningLabel(schedules, currentDay, currentLocalMinuteOfDay())
            ?: if (todaySchedule?.isClosed == true) "Сегодня выходной" else "Закрыто"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        SectionTitle("Часы работы")
        OutlinedContentCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CpIcons.Time,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(CpDimens.spacing4))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (isOpen) CpColor.Success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(CpDimens.spacing2))
                Icon(
                    imageVector = if (expanded) CpIcons.ChevronUp else CpIcons.ChevronRight,
                    contentDescription = if (expanded) "Скрыть часы работы" else "Показать часы работы",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = CpDimens.spacing2),
                    color = MaterialTheme.colorScheme.outline,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                ) {
                    orderedSchedules.forEach { schedule ->
                        ScheduleRow(
                            schedule = schedule,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsSection(
    contact: ShopContact,
    onCopyPhone: (String) -> Unit,
) {
    val contacts = buildList {
        contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
            add(ShopContactItem(
                icon = CpIcons.Phone,
                text = phone,
                onClick = { OpenInBrowser.openInBrowser("tel:$phone") },
                onCopy = { onCopyPhone(phone) },
            ))
        }
        contact.instagram?.let(::formatInstagramLink)?.let { instagram ->
            add(ShopContactItem(
                icon = CpIcons.Instagram,
                text = instagramLabel(instagram),
                onClick = { OpenInBrowser.openInBrowser(instagram.targetUrl) },
            ))
        }
        contact.website?.let(::formatWebsiteLink)?.let { website ->
            add(ShopContactItem(
                icon = CpIcons.Globe,
                text = website.displayText,
                onClick = { OpenInBrowser.openInBrowser(website.targetUrl) },
            ))
        }
        contact.email?.takeIf { it.isNotBlank() }?.let { email ->
            add(ShopContactItem(
                icon = CpIcons.Email,
                text = email,
                onClick = { OpenInBrowser.openInBrowser("mailto:$email") },
            ))
        }
    }
    if (contacts.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        SectionTitle("Контакты")
        OutlinedContentCard(contentPadding = PaddingValues(0.dp)) {
            contacts.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = CpDimens.spacing4),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                ShopContactRow(item)
            }
        }
    }
}

private data class ShopContactItem(
    val icon: ImageVector,
    val text: String,
    val onClick: () -> Unit,
    val onCopy: (() -> Unit)? = null,
)

@Composable
private fun ShopContactRow(item: ShopContactItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .heightIn(min = 56.dp)
            .padding(
                start = CpDimens.spacing4,
                end = CpDimens.spacing1,
                top = CpDimens.spacing1,
                bottom = CpDimens.spacing1,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (item.onCopy != null) {
            IconButton(onClick = item.onCopy, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = CpIcons.Copy,
                    contentDescription = "Скопировать номер",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = CpIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewsSection(
    reviews: List<Review>,
    overallRating: Double?,
    reviewCount: Int,
    shopId: String,
    shopTitle: String,
    isLoggedIn: Boolean,
    currentUserId: String?,
    onReviewPhotoClick: (List<String>, Int) -> Unit,
    onReviewHelpfulClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4)
            .padding(top = CpDimens.spacing3, bottom = CpDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) { SectionTitle("Отзывы") }
            if (reviews.isNotEmpty()) {
                IconButton(onClick = { Navigator.navigate(Navigator.Screen.ShopReviews(shopId)) }) {
                    Icon(CpIcons.ChevronRight, contentDescription = "Все отзывы")
                }
            }
        }
        if (reviews.isNotEmpty()) {
            com.coffeepeek.admin.ui.component.ReviewRatingsOverview(reviews, overallRating, reviewCount)
        }
        OutlinedContentCard(
            contentPadding = PaddingValues(if (reviews.isEmpty()) CpDimens.spacing4 else 0.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                if (reviews.isEmpty()) {
                    EmptyMascotState(
                        mascot = Res.drawable.maskot_with_book,
                        message = "Станьте первым, кто оценит и оставит отзыв о своём посещении $shopTitle",
                    )
                } else if (!isLoggedIn) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds(),
                    ) {
                        val peekWidth = if (reviews.size > 1) GuestReviewPeekWidth else 0.dp
                        val itemSpacing = if (reviews.size > 1) CpDimens.spacing3 else 0.dp
                        val cardWidth = if (reviews.size > 1) {
                            (maxWidth - itemSpacing - peekWidth).coerceAtMost(ReviewCardMaxWidth)
                        } else {
                            maxWidth
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(itemSpacing)) {
                            items(
                                count = reviews.take(3).size,
                                key = { index -> reviews[index].id },
                            ) { index ->
                                val review = reviews[index]
                                val isBlurred = index > 0
                                Box(
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .then(if (isBlurred) Modifier.blur(5.dp) else Modifier),
                                ) {
                                    ReviewCard(
                                        review = review,
                                        modifier = Modifier.fillMaxWidth(),
                                        onPhotoClick = if (isBlurred) ({ _, _ -> }) else onReviewPhotoClick,
                                        onHelpfulClick = null,
                                        showHelpfulButton = false,
                                        equalizeHeight = false,
                                    )
                                }
                            }
                        }
                    }
                    GuestAuthCard(
                        onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
                        onRegister = { Navigator.navigate(Navigator.Screen.Register) },
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val cardWidth = maxWidth.coerceAtMost(ReviewCardMaxWidth)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                        ) {
                            items(
                                count = reviews.take(3).size,
                                key = { index -> reviews[index].id },
                            ) { index ->
                                val review = reviews[index]
                                val isOwnReview = currentUserId != null && review.userId == currentUserId
                                Box(
                                    modifier = if (reviews.size > 1) {
                                        Modifier.width(cardWidth)
                                    } else {
                                        Modifier.fillParentMaxWidth()
                                    },
                                ) {
                                    ReviewCard(
                                        review = review,
                                        modifier = Modifier.fillMaxWidth(),
                                        onPhotoClick = onReviewPhotoClick,
                                        // No "helpful" on your own review.
                                        onHelpfulClick = if (isOwnReview) null else ({ onReviewHelpfulClick(review.id) }),
                                        showHelpfulButton = false,
                                        equalizeHeight = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopFeaturesSection(features: List<ShopFeatureItem>) {
    var expanded by remember(features) { mutableStateOf(false) }
    val visibleFeatures = if (expanded) features else features.take(FeaturePreviewCount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4)
            .padding(top = CpDimens.spacing6, bottom = CpDimens.spacing3),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        SectionTitle("Особенности")
        OutlinedContentCard {
            Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                visibleFeatures.forEach { feature ->
                    ShopFeatureRow(feature = feature)
                }
            }
            if (features.size > FeaturePreviewCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(CpDimens.radiusLg))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (expanded) "Свернуть" else showAllFeaturesLabel(features.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopFeatureRow(feature: ShopFeatureItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        if (feature.isBrewMethod) {
            Icon(
                painter = painterResource(brewMethodIcon(feature.name)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Icon(
                imageVector = shopTagIcon(feature.slug),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = feature.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun showAllFeaturesLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val word = when {
        mod100 in 11..14 -> "особенностей"
        mod10 == 1 -> "особенность"
        mod10 in 2..4 -> "особенности"
        else -> "особенностей"
    }
    return "Показать все $count $word"
}

@Composable
private fun CheckInsSection(
    checkIns: List<CheckIn>,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4)
            .padding(top = CpDimens.spacing6, bottom = CpDimens.spacing3),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        SectionTitle("Мои чекины")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            items(checkIns, key = { it.id }) { checkIn ->
                CheckInDisplayCard(
                    checkIn = checkIn,
                    showShopName = false,
                    onPhotoClick = onPhotoClick,
                    modifier = if (checkIns.size > 1) Modifier.width(320.dp) else Modifier.fillParentMaxWidth(),
                )
            }
        }
    }
}


@Composable
private fun OutlinedContentCard(
    contentPadding: PaddingValues = PaddingValues(CpDimens.spacing4),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            content = content,
        )
    }
}

@Composable
private fun DescriptionSection(description: String) {
    "Описание".SectionCard({
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    })
}

@Composable
private fun MenuSection(
    menu: ShopMenu,
    onPhotoClick: (Int) -> Unit,
) {
    val capturedLabel = menu.capturedAtUtc?.let(::formatMenuDate)
    val updatedLabel = menu.updatedAtUtc?.let(::formatMenuDate)
    val groups = groupedPresentItems(menu.items)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SectionTitle("Меню")
            }
        }
        OutlinedContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                verticalAlignment = Alignment.Top,
            ) {
                if (groups.isNotEmpty()) {
                    Column(modifier = Modifier.weight(1f)) {
                        groups.forEachIndexed { index, (_, drinks) ->
                            if (index > 0) {
                                Spacer(Modifier.height(CpDimens.spacing2))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                )
                                Spacer(Modifier.height(CpDimens.spacing2))
                            }
                            drinks.forEach { drink -> MenuDrinkRow(drink) }
                        }
                    }
                }

                if (menu.photos.isNotEmpty()) {
                    MenuPhotoStack(
                        photos = menu.photos.map { it.previewUrl },
                        onPhotoClick = onPhotoClick,
                    ) {
                        MenuFreshness(
                            capturedLabel = capturedLabel,
                            updatedLabel = updatedLabel,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(CpDimens.spacing2),
                            contentColor = Color.White,
                        )
                    }
                }
            }

            if (menu.photos.isEmpty()) {
                MenuFreshness(
                    capturedLabel = capturedLabel,
                    updatedLabel = updatedLabel,
                    modifier = Modifier.padding(top = CpDimens.spacing3),
                )
            }
        }
    }
}

@Composable
private fun MenuFreshness(
    capturedLabel: String?,
    updatedLabel: String?,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (capturedLabel.isNullOrBlank() && updatedLabel.isNullOrBlank()) return
    Column(modifier = modifier) {
        if (!capturedLabel.isNullOrBlank()) {
            Text(
                text = "Актуально на $capturedLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = contentColor,
            )
        }
        if (!updatedLabel.isNullOrBlank() && updatedLabel != capturedLabel) {
            Text(
                text = "Обновлено $updatedLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = contentColor,
            )
        }
    }
}

@Composable
private fun MenuDrinkRow(item: ShopMenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CpDimens.spacing1),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.nameRu.ifBlank { item.nameEn.ifBlank { item.slug } },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = CpDimens.spacing2),
        )
        item.price?.let { price ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatMenuAmount(price),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (item.currency.isBlank() || item.currency.equals("BYN", ignoreCase = true)) {
                    PriceBynIcon(
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "Белорусский рубль",
                    )
                } else {
                    Text(
                        text = item.currency,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMascotState(
    mascot: DrawableResource,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CpDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(mascot),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(CpDimens.spacing3))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ShopDetailBottomBar(
    isCheckInLoading: Boolean,
    canOpenRoute: Boolean,
    onRoute: () -> Unit,
    onReview: () -> Unit,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing3),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteIconButton(
            enabled = canOpenRoute,
            onClick = onRoute,
        )
        BottomBarAction(
            icon = CpIcons.Review,
            label = "Отзыв",
            onClick = onReview,
            modifier = Modifier.weight(1f),
        )
        BottomBarAction(
            icon = CpIcons.Check,
            label = "Чекин",
            enabled = !isCheckInLoading,
            isLoading = isCheckInLoading,
            onClick = onCheckIn,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RouteIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(CpDimens.buttonHeight)
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                if (enabled) CpColor.Primary
                else CpColor.Primary.copy(alpha = 0.38f),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CpIcons.Navigation,
            contentDescription = "Маршрут",
            tint = CpColor.DarkTextOnPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val shape = CircleShape
    Row(
        modifier = modifier
            .height(CpDimens.buttonHeight)
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CoffeePeekLoader(size = 16.dp, strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(CpDimens.spacing1))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGallery(
    photos: List<String>,
    title: String,
    onPhotoClick: (String) -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
    ) { page ->
        val photoUrl = photos[page]
        CoffeeShopImage(
            imageUrl = photoUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            placeholderLabelSize = 24.sp,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onPhotoClick(photoUrl) },
        )
    }
}

@Composable
private fun ScheduleRow(
    schedule: ShopSchedule,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Text(
            text = dayOfWeekLabel(schedule.dayOfWeek),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatScheduleHours(schedule),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CoffeeDetailsSection(
    coffeeBeans: List<String>,
    roasters: List<CatalogItem>,
    equipment: List<String>,
    favoriteRoasterIds: Set<String>,
    savingRoasterFavoriteIds: Set<String>,
    onToggleRoasterFavorite: (CatalogItem) -> Unit,
    onRoasterClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        RoasterDetailGroup(roasters, favoriteRoasterIds, savingRoasterFavoriteIds, onRoasterClick, onToggleRoasterFavorite)
        CatalogDetailGroup("Кофе", coffeeBeans)
        CatalogDetailGroup("Оборудование", equipment)
    }
}

@Composable
private fun RoasterDetailGroup(
    items: List<CatalogItem>,
    favoriteIds: Set<String>,
    savingFavoriteIds: Set<String>,
    onRoasterClick: (String) -> Unit,
    onToggleFavorite: (CatalogItem) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        SectionTitle("Обжарщики")
        OutlinedContentCard(contentPadding = PaddingValues(0.dp)) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = CpDimens.spacing4),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                RoasterLinkRow(
                    item = item,
                    isFavorite = item.roasterFavoriteId in favoriteIds,
                    isFavoriteLoading = item.roasterFavoriteId in savingFavoriteIds,
                    onClick = { item.address?.slug?.let(onRoasterClick) },
                    onToggleFavorite = { onToggleFavorite(item) },
                )
            }
        }
    }
}

@Composable
private fun RoasterLinkRow(
    item: CatalogItem,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.address != null, onClick = onClick)
            .padding(CpDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing5),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
        ) {
            val photoUrl = item.photoUrl?.takeIf(String::isNotBlank)
            if (photoUrl != null) {
                CoffeeShopImage(
                    imageUrl = photoUrl,
                    contentDescription = "Фото обжарщика ${item.name}",
                    contentScale = ContentScale.Crop,
                    placeholderLabelSize = 7.sp,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CoffeeShopPlaceholderImage(
                    labelSize = 7.sp,
                    contentDescription = "Фото обжарщика ${item.name} отсутствует",
                )
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onToggleFavorite,
                enabled = !isFavoriteLoading,
            )
            if (item.address != null) {
                Icon(
                    imageVector = CpIcons.ChevronRight,
                    contentDescription = "Открыть обжарщика ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CatalogDetailGroup(
    title: String,
    items: List<String>,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        SectionTitle(title)
        OutlinedContentCard {
            TagFlow(items = items)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlow(items: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        items.forEach { item ->
            InfoChip(item)
        }
    }
}

@Composable
private fun String.SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = 6.dp),
        shape = RoundedCornerShape(CpDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(CpDimens.spacing4)) {
            SectionTitle(this@SectionCard)
            Spacer(Modifier.height(CpDimens.spacing2))
            content()
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    containerColor: Color = CpColor.GoldWarmSoft,
    textColor: Color = CpColor.GoldWarmHover,
    onClick: (() -> Unit)? = null,
) {
    var modifier = Modifier
        .clip(RoundedCornerShape(CpDimens.radiusSm))
        .background(containerColor)
    if (onClick != null) modifier = modifier.clickable(onClick = onClick)
    Box(
        modifier = modifier
            .padding(horizontal = CpDimens.spacing2, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun instagramLabel(link: ExternalLink): String {
    val handle = link.targetUrl
        .substringAfter("instagram.com/", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .trim()
    if (handle.isBlank()) return link.displayText
    return "@$handle"
}

@Composable
private fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
    onPhotoClick: (List<String>, Int) -> Unit,
    onHelpfulClick: (() -> Unit)?,
    showHelpfulButton: Boolean = true,
    equalizeHeight: Boolean,
) {
    ReviewDisplayCard(
        review = review,
        modifier = modifier,
        onPhotoClick = onPhotoClick,
        onHelpfulClick = onHelpfulClick,
        showHelpfulButton = showHelpfulButton,
        equalizeHeight = equalizeHeight,
    )
}

private fun ShopContact.hasAny(): Boolean =
    listOf(phone, email, website, instagram).any { !it.isNullOrBlank() }

private data class ExternalLink(
    val displayText: String,
    val targetUrl: String,
)

private fun formatWebsiteLink(raw: String): ExternalLink? {
    val value = raw.trim().takeIf { it.isNotBlank() } ?: return null
    val targetUrl = ensureHttpScheme(value)
    return ExternalLink(
        displayText = prettyLinkText(targetUrl),
        targetUrl = targetUrl,
    )
}

private fun formatInstagramLink(raw: String): ExternalLink? {
    val value = raw.trim().takeIf { it.isNotBlank() } ?: return null
    val handleFromUrl = extractInstagramHandleFromUrl(value)
    val handle = (handleFromUrl ?: value.removePrefix("@"))
        .trim()
        .trim('/')
        .substringBefore('?')
        .substringBefore('#')

    if (handle.isNotBlank() && handle.matches("^[A-Za-z0-9._]{1,30}$".toRegex())) {
        val targetUrl = "https://instagram.com/$handle"
        return ExternalLink(
            displayText = "instagram.com/$handle",
            targetUrl = targetUrl,
        )
    }

    val targetUrl = ensureHttpScheme(value)
    return ExternalLink(
        displayText = prettyLinkText(targetUrl),
        targetUrl = targetUrl,
    )
}

private fun extractInstagramHandleFromUrl(raw: String): String? {
    val value = raw.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
    if (!value.startsWith("instagram.com/", ignoreCase = true)) return null
    return value
        .substringAfter("instagram.com/", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .ifBlank { null }
}

private fun ensureHttpScheme(value: String): String {
    return if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
        value
    } else {
        "https://$value"
    }
}

private fun prettyLinkText(value: String): String {
    return value
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .substringBefore('?')
        .substringBefore('#')
        .trimEnd('/')
        .ifBlank { value }
}

private fun dayOfWeekLabel(day: Int): String = when (day) {
    0 -> "Воскресенье"
    1 -> "Понедельник"
    2 -> "Вторник"
    3 -> "Среда"
    4 -> "Четверг"
    5 -> "Пятница"
    6 -> "Суббота"
    else -> "—"
}

private fun formatTime(raw: String): String {
    if (raw.isBlank()) return raw
    return raw.split(":").take(2).joinToString(":")
}

private fun formatScheduleHours(schedule: ShopSchedule): String = when {
    schedule.isClosed -> "Выходной"
    schedule.intervals.isEmpty() -> "—"
    else -> schedule.intervals.joinToString("\n") { interval ->
        "${formatTime(interval.openTime)} – ${formatTime(interval.closeTime)}"
    }
}

private fun formatReviewDate(raw: String): String {
    val datePart = utcIsoToLocalDate(raw)
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}

private fun formatMenuDate(raw: String): String = formatReviewDate(raw)

private fun formatMenuAmount(price: Double): String {
    val cents = kotlin.math.round(price * 100.0).toLong()
    val whole = cents / 100
    val frac = kotlin.math.abs(cents % 100)
    return "$whole,${frac.toString().padStart(2, '0')}"
}

private fun groupedPresentItems(items: List<ShopMenuItem>): List<Pair<String, List<ShopMenuItem>>> {
    val present = items.filter { it.availability.equals("Present", ignoreCase = true) }
    val grouped = present.groupBy { it.category }
    val order = listOf("Espresso", "Filter")
    val known = order.mapNotNull { category ->
        grouped[category]?.takeIf { it.isNotEmpty() }?.let { menuCategoryTitle(category) to it }
    }
    val rest = grouped
        .filterKeys { it !in order }
        .map { (category, drinks) -> menuCategoryTitle(category) to drinks }
    return known + rest
}

private fun menuCategoryTitle(category: String): String = when (category) {
    "Espresso" -> "Эспрессо"
    "Filter" -> "Фильтр"
    else -> category
}

private val MenuPhotoWidth = 124.dp
private val MenuPhotoHeight = 248.dp
private val MenuPhotoPeek = 12.dp
private const val MENU_PHOTO_MAX_PEEKS = 2

/**
 * Menu photos as a swipeable card stack: the current photo sits in front, the next ones peek out
 * behind it (offset + slightly scaled down) to signal there are more.
 */
@Composable
private fun MenuPhotoStack(
    photos: List<String>,
    onPhotoClick: (Int) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    SwipeablePhotoStack(
        itemCount = photos.size,
        itemWidth = MenuPhotoWidth,
        itemHeight = MenuPhotoHeight,
        peek = MenuPhotoPeek,
        maxPeeks = MENU_PHOTO_MAX_PEEKS,
        onItemClick = onPhotoClick,
    ) { page ->
            CoffeeShopImage(
                imageUrl = photos[page],
                contentDescription = "Фотография меню ${page + 1} из ${photos.size}",
                contentScale = ContentScale.Crop,
                placeholderLabelSize = 14.sp,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                        ),
                    ),
            )
            overlay()
    }
}
