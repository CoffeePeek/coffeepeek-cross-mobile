package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.rules.*
import com.coffeepeek.feature.contributions.impl.shop.*
import com.coffeepeek.feature.contributions.impl.roaster.*
import com.coffeepeek.feature.contributions.impl.change.*
import com.coffeepeek.feature.contributions.impl.ui.component.contributionPriceValue

internal fun Boolean?.nativeSignIn(): Int = when (this) { true -> 1
        false -> 0
        null -> -1 }

internal fun AddShopState.nativeForm(version: Int): NativeContributionFormState = NativeContributionFormState(
    "AddShop", version, signedIn.nativeSignIn(), catalogsLoading, loadError = "catalogs".takeIf { catalogsFailed },
    submit = when (submit) { SubmitState.Sending -> "sending"
        SubmitState.Done -> "done"
        is SubmitState.Failed -> "failed"
        else -> "idle" },
    submitMessage = (submit as? SubmitState.Failed)?.serverMessage, step = step.ordinal,
    canSubmit = signedIn == true && !catalogsLoading && !catalogsFailed && !busy && when (step) {
        AddShopStep.Basic -> basicsValid
        AddShopStep.Contacts -> contactsValid
        else -> true
    }, fields = shopFields(), groups = shopGroups(),
    photos = photos.nativePhotos(false) + menuPhotos.nativePhotos(true),
    photoLimit = ContributionLimits.SHOP_PHOTOS, menuPhotoLimit = ContributionLimits.MENU_PHOTOS,
    perDay = perDay, open = open, close = close, days = days.map { NativeContributionDay(it.day, it.closed, it.open, it.close) },
    closedDays = closedDays.toList(), locationHint = locationHint?.name, resolvingAddress = resolvingAddress,
)

private fun AddShopState.shopFields() = listOf(
    NativeContributionInput("name", name, basicsIssues.filter { it in setOf(ShopBasicsIssue.NameBlank, ShopBasicsIssue.NameTooLong) }.map { it.name }),
    NativeContributionInput("description", description),
    NativeContributionInput("address", address, basicsIssues.filter { it == ShopBasicsIssue.AddressBlank }.map { it.name }),
) + contactFields(phone, email, website, instagram, contactIssues)

private fun AddShopState.shopGroups() = listOf(
    NativeContributionGroup("city", catalogs.cities.nativeOptions(setOfNotNull(cityId))),
    NativeContributionGroup("price", PriceRange.entries.take(3).map {
        NativeContributionOption(it.name, contributionPriceValue(it.ordinal + 1).orEmpty(), it == priceRange)
    }),
    NativeContributionGroup("beans", catalogs.beans.nativeOptions(beanIds)),
    NativeContributionGroup("equipment", catalogs.equipment.nativeOptions(equipmentIds)),
    NativeContributionGroup("roasters", catalogs.roasters.nativeOptions(roasterIds)),
    NativeContributionGroup("brewMethods", catalogs.brewMethods.nativeOptions(brewMethodIds)),
)

internal fun AddRoasterState.nativeForm(version: Int): NativeContributionFormState = NativeContributionFormState(
    "AddRoaster", version, signedIn.nativeSignIn(), citiesLoading, loadError = "cities".takeIf { citiesFailed },
    submit = when (submit) { RoasterSubmitState.Sending -> "sending"
        is RoasterSubmitState.Done -> "done"
        is RoasterSubmitState.Failed -> "failed"
        else -> "idle" },
    submitMessage = (submit as? RoasterSubmitState.Failed)?.serverMessage ?: (submit as? RoasterSubmitState.Done)?.result?.message,
    resultStatus = (submit as? RoasterSubmitState.Done)?.result?.status,
    addressValidated = (submit as? RoasterSubmitState.Done)?.result?.isAddressValidated,
    canSubmit = signedIn == true && ready && !busy, fields = roasterFields(),
    groups = listOf(NativeContributionGroup("city", cities.nativeOptions(setOfNotNull(cityId)))),
    photos = photos.nativePhotos(false), photoLimit = ContributionLimits.ROASTER_PHOTOS,
)

private fun AddRoasterState.roasterFields() = listOf(
    NativeContributionInput("name", name, issues.filter { it in setOf(RoasterIssue.NameBlank, RoasterIssue.NameTooLong) }.map { it.name }),
    NativeContributionInput("description", about),
    NativeContributionInput("address", address, issues.filter { it in setOf(RoasterIssue.AddressMissing, RoasterIssue.CityMissing) }.map { it.name }),
    NativeContributionInput("instagram", instagram, listOfNotNull("Instagram".takeIf { RoasterIssue.Instagram in issues })),
    NativeContributionInput("website", website, listOfNotNull("Website".takeIf { RoasterIssue.Website in issues })),
)

internal fun SuggestChangeState.nativeForm(version: Int) = NativeContributionFormState(
    "Suggest", version, signedIn.nativeSignIn(), isLoading, title = shopTitle.orEmpty(),
    sections = ChangeSection.entries.filterNot { it == ChangeSection.Equipment }.map { it.name },
)

internal fun ChangeEditorState.nativeForm(version: Int, editing: Boolean): NativeContributionFormState = NativeContributionFormState(
    "Editor", version, signedIn.nativeSignIn(), isLoading, loadError = loadError?.name, loadMessage = loadMessage,
    submit = when (submit) { EditorSubmit.Sending -> "sending"
        is EditorSubmit.Done -> "done"
        is EditorSubmit.Failed -> "failed"
        else -> "idle" },
    submitMessage = (submit as? EditorSubmit.Failed)?.serverMessage,
    requestId = (submit as? EditorSubmit.Done)?.request?.id, resultStatus = (submit as? EditorSubmit.Done)?.request?.status?.name,
    canSubmit = canSubmit, title = shopTitle, section = section.name, editing = editing,
    fields = listOf(NativeContributionInput("description", description, listOfNotNull("DescriptionTooLong".takeIf { descriptionTooLong }))) +
        contactFields(phone, email, website, instagram, contactIssues),
    groups = editorGroups(), photos = editorPhotos(),
    menu = menuRows.map { NativeContributionMenuRow(it.slug, it.name, it.availability.name, it.priceText) },
    photoLimit = ContributionLimits.SHOP_PHOTOS, menuPhotoLimit = ContributionLimits.MENU_PHOTOS,
)

private fun ChangeEditorState.editorGroups(): List<NativeContributionGroup> = when (section) {
    ChangeSection.Tags -> listOf(NativeContributionGroup("tags", catalogs.tags.nativeOptions(tagIds)))
    ChangeSection.Roasters -> listOf(NativeContributionGroup("roasters", catalogs.roasters.nativeOptions(roasterIds)))
    ChangeSection.Equipment -> listOf(NativeContributionGroup("equipment", catalogs.equipment.nativeOptions(equipmentIds)))
    ChangeSection.BrewMethods -> listOf(NativeContributionGroup("brewMethods", catalogs.brewMethods.nativeOptions(brewMethodIds)))
    else -> emptyList()
}

private fun ChangeEditorState.editorPhotos(): List<NativeContributionPhoto> =
    retainedPhotos.nativeRetained(false) + newPhotos.nativePhotos(false) + uploadedPhotos.nativeUploaded(false) +
        retainedMenuPhotos.nativeRetained(true) + newMenuPhotos.nativePhotos(true) + uploadedMenuPhotos.nativeUploaded(true)

private fun contactFields(phone: String, email: String, website: String, instagram: String, issues: Set<ContactIssue>) =
    listOf("phone" to phone, "email" to email, "website" to website, "instagram" to instagram).map { (key, value) ->
        NativeContributionInput(key, value, issues.filter { it.name.equals(key, true) }.map { it.name })
    }

internal fun List<CatalogOption>.nativeOptions(selected: Set<String>) = map { NativeContributionOption(it.id, it.name, it.id in selected, it.photoUrl) }

internal fun PendingPhoto.nativePhotoKey(index: Int, menu: Boolean) = "new:$menu:$index:${hashCode()}"
internal fun List<PendingPhoto>.nativePhotos(menu: Boolean) = mapIndexed { index, value ->
    NativeContributionPhoto(value.nativePhotoKey(index, menu), null, value.fileName, menu)
}
private fun List<ShopPhotoRef>.nativeRetained(menu: Boolean) = map { NativeContributionPhoto("retained:$menu:${it.id}", it.url, "", menu) }
private fun List<UploadedPhoto>.nativeUploaded(menu: Boolean) = mapIndexed { index, photo ->
    NativeContributionPhoto("uploaded:$menu:$index", null, photo.fileName, menu, groupedRemoval = true)
}

