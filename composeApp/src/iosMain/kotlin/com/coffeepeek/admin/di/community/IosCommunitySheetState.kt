package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.*
import kotlinx.serialization.json.*

internal fun CommunityEditorPresentation.nativeJson() = buildJsonObject {
    put("active", true)
    put("kind", "editor")
    put("title", title)
    put("busy", busy)
    put("submitLabel", submitLabel)
    put("cancelLabel", cancelLabel)
    put("error", error)
    put("editor", editorJson())
}.toString()

private fun CommunityEditorPresentation.editorJson() = buildJsonObject {
    put("shopName", shopName)
    put("keptText", keptText)
    put("ratingsTitle", ratingsTitle)
    put("ratingsHint", ratingsHint)
    put("ratings", JsonArray(ratings.map { it.nativeJson() }))
    put("noteLabel", noteLabel)
    put("notePlaceholder", notePlaceholder)
    put("text", note)
    put("limit", noteLimit)
    put("drink", drinkJson())
}

private fun CommunityEditorPresentation.drinkJson() = buildJsonObject {
    put("label", drinkLabel)
    put("selected", selectedDrink)
    put("custom", customDrink)
    put("customLabel", customDrinkLabel)
    put("customLimit", customDrinkLimit)
    put("catalogError", catalogError)
    put("retryLabel", retryLabel)
    put("options", JsonArray(drinks.map { option -> buildJsonObject {
        put("key", option.key)
        put("label", option.label)
    } }))
}

private fun CommunityEditorRating.nativeJson() = buildJsonObject {
    put("key", key)
    put("label", label)
    put("value", value)
    put("imageUri", imageUri)
    put("descriptions", JsonArray(descriptions.map(::JsonPrimitive)))
}

internal fun CommunityReportPresentation.nativeJson() = buildJsonObject {
    put("active", true)
    put("kind", "report")
    put("title", title)
    put("busy", busy)
    put("submitLabel", submitLabel)
    put("cancelLabel", cancelLabel)
    put("error", error)
    put("report", buildJsonObject {
        put("text", text)
        put("placeholder", placeholder)
        put("limit", limit)
    })
}.toString()

internal fun CommunityEditorActions.perform(action: String, value: String) {
    when (action) {
        "text" -> text(value)
        "coffee", "service", "atmosphere" -> value.toIntOrNull()?.let { rating(action, it) }
        "drink" -> drink(value)
        "customDrink" -> customDrink(value)
        "retry" -> retry()
        "submit" -> submit()
        "dismiss" -> dismiss()
    }
}

internal fun CommunityReportActions.perform(action: String, value: String) {
    when (action) {
        "text" -> text(value)
        "submit" -> submit()
        "dismiss" -> dismiss()
    }
}
