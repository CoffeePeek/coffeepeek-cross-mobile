package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias HomeCheckInNativeDetailRenderer = @Composable (HomeCheckInDetailPresentation, HomeCheckInDetailCallbacks, Modifier) -> Boolean

data class HomeCheckInDetailPresentation(
    val card: HomeCheckInDetailCard?, val title: String, val backLabel: String,
    val active: Boolean, val dismissAllowed: Boolean, val loadingLabel: String?,
    val errorText: String?, val retryLabel: String?, val notice: String?,
    val editLabel: String, val deleteLabel: String,
    val confirmation: HomeCheckInDetailConfirmation?,
    val menuLabel: String,
)

data class HomeCheckInDetailCard(
    val key: String, val name: String, val dateLabel: String, val note: String,
    val beanName: String, val roasterName: String?, val bagUrl: String?, val recipe: String,
    val drink: String?, val publication: String?, val rejection: String?,
    val photos: List<HomeCheckInDetailPhoto>,
    val rating: Int?, val ratingLabel: String,
    val rejectedTitle: String?,
)
data class HomeCheckInDetailPhoto(val previewUrl: String, val detailUrl: String?, val label: String)
data class HomeCheckInDetailConfirmation(val title: String, val body: String, val confirmLabel: String, val cancelLabel: String)
class HomeCheckInDetailCallbacks(val actions: Map<String, () -> Unit>, val onPhoto: (Int) -> Unit)
