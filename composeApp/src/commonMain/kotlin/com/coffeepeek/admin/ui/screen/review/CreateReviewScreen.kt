package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.admin.ui.component.ReviewFormStep

import com.coffeepeek.admin.ui.component.CpTopBar

import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.ReviewRatingCards
import com.coffeepeek.admin.utils.MAX_REVIEW_PHOTOS
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReviewScreen(shopId: String) {
    val vm: CreateReviewViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CpTopBar("Новый отзыв")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            Column {
                ReviewFormStep {
                    ReviewHeaderField(state.header, vm::onHeaderChange, error = state.headerError)
                }
                ReviewFormStep {
                    ReviewCommentField(state.comment, vm::onCommentChange, error = state.commentError)
                }
                ReviewFormStep {
                    ReviewRatingCards(
                        coffeeRating = state.coffeeRating,
                        serviceRating = state.serviceRating,
                        placeRating = state.placeRating,
                        onCoffeeRatingChange = vm::onCoffeeRating,
                        onServiceRatingChange = vm::onServiceRating,
                        onPlaceRatingChange = vm::onPlaceRating,
                    )
                }
                ReviewFormStep(optional = true) {
                    PhotoAttachmentsSection(
                        photos = state.photos,
                        maxPhotos = MAX_REVIEW_PHOTOS,
                        onPhotosAdded = vm::addPhotos,
                        onRemovePhoto = vm::removePhoto,
                    )
                }

                ReviewFormStep(optional = true, last = true) {
                    ConsumedDrinkField(state.drinks, state.drinkSlug, state.customDrinkName, state.drinkName,
                        state.drinksError, vm::loadDrinks, vm::onDrinkChange)
                }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(CpDimens.spacing2))
            if (state.isSubmitting) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CoffeePeekLoader()
                }
            } else {
                AppButton(text = "Отправить на модерацию", onClick = { vm.submit() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReviewScreen(reviewId: String) {
    val vm: EditReviewViewModel = platformViewModel(parameters = { parametersOf(reviewId) })
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CpTopBar("Редактировать отзыв")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CoffeePeekLoader()
            }
            state.error != null && state.header.isBlank() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error ?: "Ошибка",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(CpDimens.spacing3))
                    AppButton(text = "Повторить", onClick = vm::loadReview)
                }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            ) {
                Column {
                    ReviewFormStep {
                        ReviewHeaderField(state.header, vm::onHeaderChange, error = state.headerError)
                    }
                    ReviewFormStep {
                        ReviewCommentField(state.comment, vm::onCommentChange, error = state.commentError)
                    }
                    ReviewFormStep {
                        ReviewRatingCards(
                            coffeeRating = state.coffeeRating,
                            serviceRating = state.serviceRating,
                            placeRating = state.placeRating,
                            onCoffeeRatingChange = vm::onCoffeeRating,
                            onServiceRatingChange = vm::onServiceRating,
                            onPlaceRatingChange = vm::onPlaceRating,
                        )
                    }
                    ReviewFormStep(optional = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            ExistingReviewPhotos(state.existingPhotoUrls, onPhotoClick = {})
                            PhotoAttachmentsSection(
                                photos = state.newPhotos,
                                maxPhotos = MAX_REVIEW_PHOTOS,
                                onPhotosAdded = vm::addPhotos,
                                onRemovePhoto = vm::removeNewPhoto,
                                title = "Новые фото",
                                hint = "Добавьте новые фото (до $MAX_REVIEW_PHOTOS). Существующие фото останутся без изменений.",
                            )
                        }
                    }

                    ReviewFormStep(optional = true, last = true) {
                        ConsumedDrinkField(state.drinks, state.drinkSlug, state.customDrinkName, state.drinkName,
                            state.drinksError, vm::loadDrinks, vm::onDrinkChange)
                    }
                }

                state.error?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(CpDimens.spacing2))
                if (state.isSubmitting) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CoffeePeekLoader()
                    }
                } else {
                    AppButton(
                        text = "Сохранить изменения",
                        onClick = { vm.submit() },
                        enabled = state.canEdit,
                    )
                }
            }
        }
    }
}
