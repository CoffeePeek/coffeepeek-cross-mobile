package com.coffeepeek.feature.community.api

/** Rendering values only; the feature's existing editor remains the sole writer. */
data class CommunityEditorPresentation(
    val title: String, val shopName: String, val keptText: String,
    val ratingsTitle: String, val ratingsHint: String, val ratings: List<CommunityEditorRating>,
    val noteLabel: String, val notePlaceholder: String, val note: String, val noteLimit: Int,
    val drinkLabel: String, val drinks: List<CommunityEditorDrink>, val selectedDrink: String,
    val customDrink: String?, val customDrinkLabel: String, val customDrinkLimit: Int,
    val catalogError: String?, val retryLabel: String, val error: String?,
    val submitLabel: String, val cancelLabel: String, val busy: Boolean,
)

data class CommunityEditorRating(
    val key: String, val label: String, val value: Int, val imageUri: String,
    val descriptions: List<String>,
)

data class CommunityEditorDrink(val key: String, val label: String)

class CommunityEditorActions(
    val text: (String) -> Unit, val rating: (String, Int) -> Unit,
    val drink: (String) -> Unit, val customDrink: (String) -> Unit,
    val retry: () -> Unit, val submit: () -> Unit, val dismiss: () -> Unit,
)
