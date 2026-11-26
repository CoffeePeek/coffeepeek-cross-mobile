package com.coffeepeek.admin.di.contributions

import com.coffeepeek.feature.contributions.impl.*
import kotlinx.serialization.json.*

internal fun NativeContributionFormState.iosJson(): String = buildJsonObject {
    formStatus(this)
    formControls(this)
    put("fields", buildJsonArray { fields.forEach { add(it.fieldJson()) } })
    put("groups", buildJsonArray { groups.forEach { add(it.groupJson()) } })
    put("sections", stringsJson(sections))
    put("photos", buildJsonArray { photos.forEach { add(it.photoJson()) } })
    put("menu", buildJsonArray { menu.forEach { add(it.menuJson()) } })
    put("days", buildJsonArray { days.forEach { add(it.dayJson()) } })
    put("closedDays", buildJsonArray { closedDays.forEach { add(it) } })
    put("map", map?.mapJson() ?: JsonNull)
}.toString()

private fun NativeContributionFormState.formStatus(builder: JsonObjectBuilder) = with(builder) {
    put("kind", kind)
    put("accountVersion", accountVersion)
    put("signedIn", signedIn)
    put("loading", loading)
    put("loadError", loadError)
    put("loadMessage", loadMessage)
    put("submit", submit)
    put("submitMessage", submitMessage)
    put("step", step)
    put("canSubmit", canSubmit)
    put("title", title)
    put("section", section)
    put("requestId", requestId)
    put("resultStatus", resultStatus)
    put("editing", editing)
    put("hasEdits", hasEdits)
    put("addressValidated", addressValidated)
}

private fun NativeContributionFormState.formControls(builder: JsonObjectBuilder) = with(builder) {
    put("photoLimit", photoLimit)
    put("menuPhotoLimit", menuPhotoLimit)
    put("perDay", perDay)
    put("open", open)
    put("close", close)
    put("locationHint", locationHint)
    put("resolvingAddress", resolvingAddress)
}

private fun stringsJson(values: List<String>) = buildJsonArray { values.forEach { add(it) } }

private fun NativeContributionInput.fieldJson() = buildJsonObject {
    put("key", key)
    put("value", value)
    put("issues", stringsJson(issues))
}
private fun NativeContributionGroup.groupJson() = buildJsonObject {
    put("key", key)
    put("options", buildJsonArray { options.forEach { add(it.optionJson()) } })
}
private fun NativeContributionOption.optionJson() = buildJsonObject {
    put("id", id)
    put("name", name)
    put("selected", selected)
    put("photoUrl", photoUrl)
}
private fun NativeContributionPhoto.photoJson() = buildJsonObject {
    put("key", key)
    put("url", url)
    put("fileName", fileName)
    put("menu", menu)
    put("groupedRemoval", groupedRemoval)
}
private fun NativeContributionMenuRow.menuJson() = buildJsonObject {
    put("slug", slug)
    put("name", name)
    put("availability", availability)
    put("price", price)
}
private fun NativeContributionDay.dayJson() = buildJsonObject {
    put("day", day)
    put("closed", closed)
    put("open", open)
    put("close", close)
}
private fun NativeContributionMap.mapJson() = buildJsonObject {
    put("latitude", latitude)
    put("longitude", longitude)
    put("cameraLatitude", cameraLatitude)
    put("cameraLongitude", cameraLongitude)
    put("address", address)
    put("resolving", resolving)
    put("failed", failed)
    put("canConfirm", canConfirm)
}
