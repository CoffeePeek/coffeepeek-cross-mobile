package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.ReviewRatingCards
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.screen.review.ConsumedDrinkField
import com.coffeepeek.domain.model.CheckInVisibility

@Composable
internal fun EditCheckInSheet(state: CommunityUiState, vm: CommunityViewModel) {
    val edit = state.editing ?: return
    SwipeDismissModalBottomSheet(onDismissRequest = vm::dismissEdit) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            Text("Редактировать чек-ин", style = MaterialTheme.typography.headlineSmall)
            Text(edit.source.shopName.ifBlank { "Кофейня" }, style = MaterialTheme.typography.titleMedium)
            Text(
                "Кофейня, дата визита и фотографии сохранятся.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (edit.source.visibility == CheckInVisibility.Public) Text(
                "После сохранения публичный чек-ин снова отправится на проверку.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReviewRatingCards(
                coffeeRating = edit.rating.coffee, serviceRating = edit.rating.service, placeRating = edit.rating.place,
                onCoffeeRatingChange = { vm.updateEdit(edit.copy(rating = edit.rating.copy(coffee = it))) },
                onServiceRatingChange = { vm.updateEdit(edit.copy(rating = edit.rating.copy(service = it))) },
                onPlaceRatingChange = { vm.updateEdit(edit.copy(rating = edit.rating.copy(place = it))) },
            )
            Text("Текст чек-ина", style = MaterialTheme.typography.labelMedium)
            ReviewTextInput(
                value = edit.text, onValueChange = { vm.updateEdit(edit.copy(text = it)) },
                placeholder = "Расскажите о визите", maxLength = 1000, isError = edit.error != null,
            )
            val unchangedDrink = edit.drinkSlug == edit.source.drinkSlug
            ConsumedDrinkField(
                drinks = state.drinks, slug = edit.drinkSlug, customName = edit.customDrinkName,
                savedName = edit.source.drinkNameRu.takeIf { unchangedDrink },
                savedNameEn = edit.source.drinkNameEn.takeIf { unchangedDrink },
                error = state.drinksError, onRetry = vm::loadDrinks,
                onChange = { slug, name -> vm.updateEdit(edit.copy(drinkSlug = slug, customDrinkName = name)) },
            )
            edit.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AppButton(if (state.isSaving) "Сохраняем…" else "Сохранить", onClick = vm::saveEdit, enabled = !state.isSaving)
            TextButton(onClick = vm::dismissEdit, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
        }
    }
}
