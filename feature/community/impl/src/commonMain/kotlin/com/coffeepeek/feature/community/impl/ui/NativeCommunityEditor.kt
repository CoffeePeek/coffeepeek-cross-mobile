package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.rules.*
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.PostEditState

internal data class NativeEditorTexts(
    val title: String, val shop: String, val kept: String, val ratingsTitle: String, val ratingsHint: String,
    val note: String, val notePlaceholder: String, val drink: String, val none: String, val other: String,
    val customDrink: String, val catalogError: String, val retry: String, val saving: String, val save: String, val cancel: String,
)

internal fun nativeEditorPresentation(edit: PostEditState, drinks: List<DrinkOption>, failed: Boolean,
    saving: Boolean, language: String, texts: NativeEditorTexts, ratings: List<CommunityEditorRating>, error: String?): CommunityEditorPresentation {
    val choices = nativeEditorDrinks(edit, drinks, language, texts.none, texts.other)
    return CommunityEditorPresentation(
        title = texts.title, shopName = edit.source.shopName.ifBlank { texts.shop }, keptText = texts.kept,
        ratingsTitle = texts.ratingsTitle, ratingsHint = texts.ratingsHint, ratings = ratings,
        noteLabel = texts.note, notePlaceholder = texts.notePlaceholder, note = edit.text, noteLimit = MAX_NOTE_LENGTH,
        drinkLabel = texts.drink, drinks = choices,
        selectedDrink = nativeDrinkKey(edit.drinkSlug),
        customDrink = edit.customDrinkName.orEmpty().takeIf { edit.drinkSlug == OTHER_DRINK },
        customDrinkLabel = texts.customDrink, customDrinkLimit = MAX_DRINK_NAME_LENGTH,
        catalogError = texts.catalogError.takeIf { failed }, retryLabel = texts.retry, error = error,
        submitLabel = if (saving) texts.saving else texts.save, cancelLabel = texts.cancel, busy = saving,
    )
}

internal fun nativeEditorAxes(edit: PostEditState) = listOf(
    "coffee" to edit.rating.coffee, "service" to edit.rating.service, "atmosphere" to edit.rating.place,
)

internal fun nativeEditorRatingResource(key: String) = when (key) {
    "coffee" -> Res.string.community_rating_coffee
    "service" -> Res.string.community_rating_service
    "atmosphere" -> Res.string.community_rating_atmosphere
    else -> error("Unknown community rating axis: $key")
}

internal fun nativeEditorDrinks(edit: PostEditState, drinks: List<DrinkOption>, language: String,
    none: String, other: String): List<CommunityEditorDrink> {
    val selected = drinks.find { it.slug == edit.drinkSlug }
    val title = nativeSavedDrinkTitle(edit, language) ?: selected?.nativeTitle(language)
        ?: if (edit.drinkSlug == OTHER_DRINK) other else edit.drinkSlug.orEmpty()
    return buildList {
        add(CommunityEditorDrink(nativeDrinkKey(null), none))
        if (edit.drinkSlug != null && selected == null) add(CommunityEditorDrink(nativeDrinkKey(edit.drinkSlug), title))
        drinks.forEach { option ->
            add(CommunityEditorDrink(nativeDrinkKey(option.slug),
                if (option.slug == edit.drinkSlug) title else option.nativeTitle(language)))
        }
    }
}

private fun nativeSavedDrinkTitle(edit: PostEditState, language: String): String? {
    if (edit.drinkSlug == OTHER_DRINK) return edit.customDrinkName?.takeIf(String::isNotBlank)
    val saved = edit.source.drink?.takeIf { it.slug == edit.drinkSlug } ?: return null
    val name = if (language == "en") saved.nameEn?.takeIf(String::isNotBlank) ?: saved.nameRu
        else saved.nameRu?.takeIf(String::isNotBlank) ?: saved.nameEn
    return name?.takeIf(String::isNotBlank)
}

private fun DrinkOption.nativeTitle(language: String) =
    if (language == "en") nameEn.ifBlank { nameRu } else nameRu.ifBlank { nameEn }

internal fun nativeDrinkKey(slug: String?) = slug?.let { "drink:$it" } ?: "none"

internal fun nativeEditorActions(edit: PostEditState, drinks: List<DrinkOption>, callbacks: PostEditorCallbacks) = CommunityEditorActions(
    text = callbacks.onText, rating = { key, value ->
        nativeRating(edit, key, value)?.let(callbacks.onRating)
    },
    drink = { key ->
        val slug = key.removePrefix("drink:").takeIf { key.startsWith("drink:") }
        if (key == "none" || slug != null && slug != edit.drinkSlug && drinks.any { it.slug == slug }) {
            callbacks.onDrink(slug, "".takeIf { slug == OTHER_DRINK })
        }
    },
    customDrink = { if (edit.drinkSlug == OTHER_DRINK) callbacks.onDrink(OTHER_DRINK, it.take(MAX_DRINK_NAME_LENGTH)) },
    retry = callbacks.onReloadDrinks, submit = callbacks.onSave, dismiss = callbacks.onDismiss,
)

private fun nativeRating(edit: PostEditState, key: String, value: Int) = if (value !in 1..5) null else when (key) {
    "coffee" -> edit.rating.copy(coffee = value)
    "service" -> edit.rating.copy(service = value)
    "atmosphere" -> edit.rating.copy(place = value)
    else -> null
}
