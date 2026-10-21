package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgeColors
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.ChangeSummary
import com.coffeepeek.feature.contributions.impl.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class Visual(val icon: ImageVector, val colors: CoffeePeekIconBadgeColors)

internal fun ContributionKind.visual(section: ChangeSection?): Visual = when (this) {
    ContributionKind.Shops -> Visual(CoffeePeekIcons.Coffee, CoffeePeekIconBadgePalette.Gold)
    ContributionKind.Roasters -> Visual(CoffeePeekIcons.CoffeeBean, CoffeePeekIconBadgePalette.Rose)
    ContributionKind.Changes -> section?.visual() ?: Visual(CoffeePeekIcons.Edit, CoffeePeekIconBadgePalette.Violet)
}

internal fun ChangeSection.visual(): Visual = when (this) {
    ChangeSection.Photos -> Visual(CoffeePeekIcons.Photo, CoffeePeekIconBadgePalette.Sky)
    ChangeSection.Contacts -> Visual(CoffeePeekIcons.Phone, CoffeePeekIconBadgePalette.Mint)
    ChangeSection.Description -> Visual(CoffeePeekIcons.NoteEdit, CoffeePeekIconBadgePalette.Lavender)
    ChangeSection.Tags -> Visual(CoffeePeekIcons.Sparkle, CoffeePeekIconBadgePalette.Gold)
    ChangeSection.Roasters -> Visual(CoffeePeekIcons.CoffeeBean, CoffeePeekIconBadgePalette.Rose)
    ChangeSection.Equipment -> Visual(CoffeePeekIcons.Settings, CoffeePeekIconBadgePalette.Cyan)
    ChangeSection.Menu -> Visual(CoffeePeekIcons.Menu, CoffeePeekIconBadgePalette.Emerald)
    ChangeSection.BrewMethods -> Visual(CoffeePeekIcons.Coffee, CoffeePeekIconBadgePalette.Aqua)
}

internal fun ModerationStatus.color(): Color = when (this) {
    ModerationStatus.Approved -> CoffeePeekColors.Success
    ModerationStatus.Pending -> CoffeePeekColors.GoldWarmHover
    ModerationStatus.Rejected -> CoffeePeekColors.Error
}

@Composable internal fun ContributionKind.title() = stringResource(
    when (this) {
        ContributionKind.Shops -> R.string.contributions_title_shops
        ContributionKind.Roasters -> R.string.contributions_title_roasters
        ContributionKind.Changes -> R.string.contributions_title_changes
    },
)

@Composable internal fun ContributionKind.emptyText() = stringResource(
    when (this) {
        ContributionKind.Shops -> R.string.contributions_empty_shops
        ContributionKind.Roasters -> R.string.contributions_empty_roasters
        ContributionKind.Changes -> R.string.contributions_empty_changes
    },
)

@Composable internal fun ModerationStatus.tabTitle() = stringResource(
    when (this) {
        ModerationStatus.Approved -> R.string.contributions_tab_approved
        ModerationStatus.Pending -> R.string.contributions_tab_pending
        ModerationStatus.Rejected -> R.string.contributions_tab_rejected
    },
)

@Composable internal fun ModerationStatus.shortTitle() = stringResource(
    when (this) {
        ModerationStatus.Approved -> R.string.contributions_status_approved
        ModerationStatus.Pending -> R.string.contributions_status_pending
        ModerationStatus.Rejected -> R.string.contributions_status_rejected
    },
)

@Composable internal fun ChangeSection.title() = stringResource(
    when (this) {
        ChangeSection.Photos -> R.string.contributions_section_photos
        ChangeSection.Contacts -> R.string.contributions_section_contacts
        ChangeSection.Description -> R.string.contributions_section_description
        ChangeSection.Tags -> R.string.contributions_section_tags
        ChangeSection.Roasters -> R.string.contributions_section_roasters
        ChangeSection.Equipment -> R.string.contributions_section_equipment
        ChangeSection.Menu -> R.string.contributions_section_menu
        ChangeSection.BrewMethods -> R.string.contributions_section_brew_methods
    },
)

@Composable internal fun ChangeSection.hint() = stringResource(
    when (this) {
        ChangeSection.Photos -> R.string.contributions_hint_photos
        ChangeSection.Contacts -> R.string.contributions_hint_contacts
        ChangeSection.Description -> R.string.contributions_hint_description
        ChangeSection.Tags -> R.string.contributions_hint_tags
        ChangeSection.Roasters -> R.string.contributions_hint_roasters
        ChangeSection.Equipment -> R.string.contributions_hint_equipment
        ChangeSection.Menu -> R.string.contributions_hint_menu
        ChangeSection.BrewMethods -> R.string.contributions_hint_brew_methods
    },
)

/** The one-line summary of a change request; a request without anything to describe shows the hint of its section. */
@Composable internal fun ChangeSummary.text(section: ChangeSection): String = when (this) {
    ChangeSummary.DescriptionCleared -> stringResource(R.string.contributions_summary_description_cleared)
    ChangeSummary.Contacts -> stringResource(R.string.contributions_summary_contacts)
    is ChangeSummary.Photos -> stringResource(R.string.contributions_summary_photos, retained, added)
    is ChangeSummary.Selected -> stringResource(
        when (this.section) {
            ChangeSection.Roasters -> R.string.contributions_summary_roasters
            ChangeSection.Equipment -> R.string.contributions_summary_equipment
            ChangeSection.BrewMethods -> R.string.contributions_summary_brew_methods
            else -> R.string.contributions_summary_tags
        },
        count,
    )
    is ChangeSummary.Menu -> stringResource(R.string.contributions_summary_menu, items, newPhotos)
    ChangeSummary.Empty -> section.hint()
}

@Composable internal fun Availability.title() = stringResource(
    when (this) {
        Availability.Present -> R.string.contributions_availability_present
        Availability.Absent -> R.string.contributions_availability_absent
        Availability.Unknown -> R.string.contributions_availability_unknown
    },
)

private val localFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/** `dd.MM.yyyy HH:mm` in the device's time zone; a value that is not a UTC timestamp is shown as it came. */
internal fun localDateTime(iso: String, zone: ZoneId = ZoneId.systemDefault()): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC) }.getOrNull()
        ?: return iso
    return instant.atZone(zone).format(localFormat)
}
