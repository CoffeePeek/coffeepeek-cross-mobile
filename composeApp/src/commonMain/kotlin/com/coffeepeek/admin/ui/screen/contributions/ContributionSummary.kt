package com.coffeepeek.admin.ui.screen.contributions

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.domain.model.ShopChangePayload

data class ContributionLoadError(val serverMessage: String?) {
    fun messageOr(fallback: String): String = serverMessage ?: fallback
}

sealed interface ContributionSummary {
    data class Description(val text: String) : ContributionSummary
    data object DescriptionCleared : ContributionSummary
    data object Contacts : ContributionSummary
    data class Photos(val retained: Int, val added: Int) : ContributionSummary
    data class Tags(val count: Int) : ContributionSummary
    data class Roasters(val count: Int) : ContributionSummary
    data class Equipment(val count: Int) : ContributionSummary
    data class Methods(val count: Int) : ContributionSummary
    data class Menu(val items: Int, val photos: Int) : ContributionSummary
    data object SectionHint : ContributionSummary
}

internal fun ShopChangePayload.contributionSummary(): ContributionSummary {
    description?.let { return if (it.isBlank()) ContributionSummary.DescriptionCleared else ContributionSummary.Description(it) }
    if (contacts != null) return ContributionSummary.Contacts
    photos?.let { return ContributionSummary.Photos(it.retainedPhotoIds.size, it.newPhotos.size) }
    tagIds?.let { return ContributionSummary.Tags(it.size) }
    roasterIds?.let { return ContributionSummary.Roasters(it.size) }
    equipmentIds?.let { return ContributionSummary.Equipment(it.size) }
    brewMethodIds?.let { return ContributionSummary.Methods(it.size) }
    menu?.let { return ContributionSummary.Menu(it.items.size, it.newPhotos.size) }
    return ContributionSummary.SectionHint
}

internal data class ContributionSummaryLabels(
    val descriptionCleared: String,
    val contacts: String,
    val photos: String,
    val tags: String,
    val roasters: String,
    val equipment: String,
    val methods: String,
    val menu: String,
)

@Composable
internal fun contributionSummaryLabels() = ContributionSummaryLabels(
    stringResource(Res.string.legacy_contribution_description_cleared),
    stringResource(Res.string.legacy_contribution_contacts),
    stringResource(Res.string.legacy_contribution_photos),
    stringResource(Res.string.legacy_contribution_tags),
    stringResource(Res.string.legacy_contribution_roasters),
    stringResource(Res.string.legacy_contribution_equipment),
    stringResource(Res.string.legacy_contribution_methods),
    stringResource(Res.string.legacy_contribution_menu),
)

internal fun ContributionSummary.label(labels: ContributionSummaryLabels, fallback: String): String = when (this) {
    is ContributionSummary.Description -> text
    ContributionSummary.DescriptionCleared -> labels.descriptionCleared
    ContributionSummary.Contacts -> labels.contacts
    is ContributionSummary.Photos -> labels.photos.replace("%1\$s", retained.toString()).replace("%2\$s", added.toString())
    is ContributionSummary.Tags -> labels.tags.replace("%1\$s", count.toString())
    is ContributionSummary.Roasters -> labels.roasters.replace("%1\$s", count.toString())
    is ContributionSummary.Equipment -> labels.equipment.replace("%1\$s", count.toString())
    is ContributionSummary.Methods -> labels.methods.replace("%1\$s", count.toString())
    is ContributionSummary.Menu -> labels.menu.replace("%1\$s", items.toString()).replace("%2\$s", photos.toString())
    ContributionSummary.SectionHint -> fallback
}
