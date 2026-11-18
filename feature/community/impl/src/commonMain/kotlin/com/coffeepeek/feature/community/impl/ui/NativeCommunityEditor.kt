package com.coffeepeek.feature.community.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.rules.*
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.PostEditState

@Composable
internal fun nativeEditorPresentation(edit: PostEditState, drinks: List<DrinkOption>, failed: Boolean,
    saving: Boolean): CommunityEditorPresentation {
    val choices = nativeEditorDrinks(edit, drinks, Locale.current.language,
        stringResource(Res.string.community_drink_none), stringResource(Res.string.community_drink_other))
    return CommunityEditorPresentation(
        title = stringResource(Res.string.community_edit_title),
        shopName = edit.source.shopName.ifBlank { stringResource(Res.string.community_shop_default) },
        keptText = stringResource(Res.string.community_edit_kept),
        ratingsTitle = stringResource(Res.string.community_ratings_title),
        ratingsHint = stringResource(Res.string.community_ratings_hint), ratings = nativeEditorRatings(edit),
        noteLabel = stringResource(Res.string.community_note_label),
        notePlaceholder = stringResource(Res.string.community_note_placeholder), note = edit.text, noteLimit = MAX_NOTE_LENGTH,
        drinkLabel = stringResource(Res.string.community_drink_label), drinks = choices,
        selectedDrink = nativeDrinkKey(edit.drinkSlug),
        customDrink = edit.customDrinkName.orEmpty().takeIf { edit.drinkSlug == OTHER_DRINK },
        customDrinkLabel = stringResource(Res.string.community_drink_custom), customDrinkLimit = MAX_DRINK_NAME_LENGTH,
        catalogError = stringResource(Res.string.community_drink_error).takeIf { failed },
        retryLabel = stringResource(Res.string.community_drink_retry), error = edit.error?.let { editErrorText(it) },
        submitLabel = stringResource(if (saving) Res.string.community_saving else Res.string.community_save),
        cancelLabel = stringResource(Res.string.community_cancel), busy = saving,
    )
}

@Composable
private fun nativeEditorRatings(edit: PostEditState): List<CommunityEditorRating> = listOf(
    nativeEditorRating("coffee", stringResource(Res.string.community_rating_coffee), edit.rating.coffee),
    nativeEditorRating("service", stringResource(Res.string.community_rating_service), edit.rating.service),
    nativeEditorRating("atmosphere", stringResource(Res.string.community_rating_atmosphere), edit.rating.place),
)

@Composable
private fun nativeEditorRating(key: String, label: String, value: Int) = CommunityEditorRating(
    key, label, value, Res.getUri("drawable/community_rating_$key.png"),
    (1..5).map { stringResource(Res.string.community_choose_rating, label, it) },
)

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
    return if (language == "en") saved.nameEn?.takeIf(String::isNotBlank) ?: saved.nameRu
        else saved.nameRu?.takeIf(String::isNotBlank) ?: saved.nameEn
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
