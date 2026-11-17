package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.domain.model.ChangeMenu
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.list.ContributionItem
import com.coffeepeek.feature.contributions.impl.list.ContributionNote
import com.coffeepeek.feature.contributions.impl.list.ContributionTarget
import com.coffeepeek.feature.contributions.impl.list.ContributionsListState
import com.coffeepeek.feature.contributions.impl.list.ListError
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestState
import com.coffeepeek.feature.contributions.impl.request.RequestError
import com.coffeepeek.feature.contributions.impl.ui.localDateTime

/** Resource suffixes and raw presentation values, never a request payload or account identity. */
data class NativeContributionText(val key: String, val count: Int = 0, val extra: Int = 0)

data class NativeContributionsListState(
    val loading: Boolean = true,
    val error: String? = null,
    val serverMessage: String? = null,
    val tabs: List<NativeContributionTab> = emptyList(),
    val selected: String? = null,
    val items: List<NativeContributionItem> = emptyList(),
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
)

data class NativeContributionTab(val status: String, val count: Int)

data class NativeContributionItem(
    val id: String, val title: String?, val subtitle: String?, val section: String?,
    val summary: NativeContributionText?, val date: String?, val waiting: Boolean,
    val reason: String?, val canOpen: Boolean,
)

data class NativeChangeRequestState(
    val loading: Boolean = true,
    val error: String? = null,
    val serverMessage: String? = null,
    val section: String? = null,
    val status: String? = null,
    val reason: String? = null,
    val date: String? = null,
    val fields: List<NativeContributionField> = emptyList(),
    val note: String? = null,
    val canOpenShop: Boolean = false,
)

data class NativeContributionField(
    val labelKey: String? = null,
    val label: String? = null,
    val valueKey: String? = null,
    val value: String? = null,
    val price: String? = null,
)

internal fun ContributionsListState.toNative(): NativeContributionsListState {
    val visible = if (isLoading || error != null) emptyList() else visibleTabs
    val tab = tabs[selectedTab].takeIf { visible.isNotEmpty() }
    return NativeContributionsListState(
        loading = isLoading, error = error?.let { if (it == ListError.SignInRequired) "sign_in_prompt" else "load_failed" },
        serverMessage = serverMessage,
        tabs = visible.map { NativeContributionTab(it.key(), tabs.getValue(it).totalCount) },
        selected = selectedTab?.key().takeIf { tab != null }, items = tab?.items.orEmpty().map { it.toNative() },
        hasMore = tab?.hasMore == true, loadingMore = tab?.isLoadingMore == true, moreFailed = tab?.loadMoreFailed == true,
    )
}

internal fun ModerationStatus.key(): String = name.lowercase()

internal fun ContributionItem.toNative() = NativeContributionItem(
    id, title, subtitle, section?.key(), summary?.toNative(), createdAtUtc?.let { localDateTime(it) },
    note == ContributionNote.WaitingForCatalog, rejectedReason, target != null,
)

internal fun ContributionTarget.destination(): Pair<Boolean, String> = when (this) {
    is ContributionTarget.Shop -> true to slug
    is ContributionTarget.Request -> false to id
}

internal fun ChangeRequestState.toNative(): NativeChangeRequestState {
    val value = request
    return NativeChangeRequestState(
        isLoading, error?.key(), serverMessage, value?.section?.key(), value?.status?.key(),
        value?.rejectionReason, value?.createdAtUtc?.let(::localDateTime), value?.fields().orEmpty(),
        value?.section?.note(), value?.shopSlug?.isNotBlank() == true,
    )
}

private fun RequestError.key() = when (this) {
    RequestError.SignInRequired -> "sign_in_prompt"
    RequestError.NotFound -> "request_not_found"
    RequestError.LoadFailed -> "load_failed"
}

private fun ChangeSection.key() = when (this) {
    ChangeSection.BrewMethods -> "brew_methods"
    else -> name.lowercase()
}

private fun ChangeSummary.toNative(): NativeContributionText? = when (this) {
    ChangeSummary.DescriptionCleared -> NativeContributionText("summary_description_cleared")
    ChangeSummary.Contacts -> NativeContributionText("summary_contacts")
    is ChangeSummary.Photos -> NativeContributionText("summary_photos", retained, added)
    is ChangeSummary.Selected -> NativeContributionText("summary_${section.key()}", count)
    is ChangeSummary.Menu -> NativeContributionText("summary_menu", items, newPhotos)
    ChangeSummary.Empty -> null
}

private fun ChangeRequest.fields(): List<NativeContributionField> = when (section) {
    ChangeSection.Description -> listOf(
        payload.description?.takeIf(String::isNotBlank)?.let { NativeContributionField(value = it) }
            ?: NativeContributionField(valueKey = "request_description_removed"),
    )
    ChangeSection.Contacts -> payload.contacts?.fields().orEmpty()
    ChangeSection.Photos -> listOf(
        count("request_retained", payload.photos?.retainedIds?.size ?: 0),
        count("request_added", payload.photos?.uploaded?.size ?: 0),
    )
    ChangeSection.Tags -> listOf(count("request_selected_tags", payload.tagIds?.size ?: 0))
    ChangeSection.Roasters -> listOf(count("request_selected_roasters", payload.roasterIds?.size ?: 0))
    ChangeSection.Equipment -> listOf(count("request_selected_equipment", payload.equipmentIds?.size ?: 0))
    ChangeSection.BrewMethods -> listOf(count("request_selected_brew_methods", payload.brewMethodIds?.size ?: 0))
    ChangeSection.Menu -> payload.menu?.nativeFields().orEmpty()
}

private fun Contacts.fields() = listOf(
    contact("request_phone", phone), contact("request_email", email),
    contact("request_site", website), contact("request_instagram", instagram),
)

private fun contact(key: String, value: String?) = value?.takeIf(String::isNotBlank)?.let {
    NativeContributionField(labelKey = key, value = it)
} ?: NativeContributionField(labelKey = key, valueKey = "request_removed")

private fun count(key: String, value: Int) = NativeContributionField(labelKey = key, value = value.toString())

private fun ChangeMenu.nativeFields() = listOf(
    count("request_menu_items", items.size), count("request_menu_photos", retainedPhotoIds.size + uploaded.size),
) + items.map {
    NativeContributionField(label = it.slug, valueKey = "availability_${it.availability.name.lowercase()}", price = it.price?.toString())
}

private fun ChangeSection.note() = when (this) {
    ChangeSection.Photos -> "request_gallery_note"
    ChangeSection.Tags, ChangeSection.Roasters, ChangeSection.Equipment, ChangeSection.BrewMethods -> "request_set_note"
    else -> null
}
