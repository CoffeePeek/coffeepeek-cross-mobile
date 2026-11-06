package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_availability_absent
import com.coffeepeek.feature.contributions.impl.resources.contributions_availability_present
import com.coffeepeek.feature.contributions.impl.resources.contributions_availability_unknown
import com.coffeepeek.feature.contributions.impl.resources.contributions_empty_changes
import com.coffeepeek.feature.contributions.impl.resources.contributions_empty_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_empty_shops
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_brew_methods
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_contacts
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_description
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_equipment
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_menu
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_hint_tags
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_brew_methods
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_contacts
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_description
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_equipment
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_menu
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_tags
import com.coffeepeek.feature.contributions.impl.resources.contributions_status_approved
import com.coffeepeek.feature.contributions.impl.resources.contributions_status_pending
import com.coffeepeek.feature.contributions.impl.resources.contributions_status_rejected
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_brew_methods
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_contacts
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_description_cleared
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_equipment
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_menu
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_summary_tags
import com.coffeepeek.feature.contributions.impl.resources.contributions_tab_approved
import com.coffeepeek.feature.contributions.impl.resources.contributions_tab_pending
import com.coffeepeek.feature.contributions.impl.resources.contributions_tab_rejected
import com.coffeepeek.feature.contributions.impl.resources.contributions_title_changes
import com.coffeepeek.feature.contributions.impl.resources.contributions_title_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_title_shops
import com.coffeepeek.core.datetime.parseServerInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgeColors
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.ChangeSummary

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
        ContributionKind.Shops -> Res.string.contributions_title_shops
        ContributionKind.Roasters -> Res.string.contributions_title_roasters
        ContributionKind.Changes -> Res.string.contributions_title_changes
    },
)

@Composable internal fun ContributionKind.emptyText() = stringResource(
    when (this) {
        ContributionKind.Shops -> Res.string.contributions_empty_shops
        ContributionKind.Roasters -> Res.string.contributions_empty_roasters
        ContributionKind.Changes -> Res.string.contributions_empty_changes
    },
)

@Composable internal fun ModerationStatus.tabTitle() = stringResource(
    when (this) {
        ModerationStatus.Approved -> Res.string.contributions_tab_approved
        ModerationStatus.Pending -> Res.string.contributions_tab_pending
        ModerationStatus.Rejected -> Res.string.contributions_tab_rejected
    },
)

@Composable internal fun ModerationStatus.shortTitle() = stringResource(
    when (this) {
        ModerationStatus.Approved -> Res.string.contributions_status_approved
        ModerationStatus.Pending -> Res.string.contributions_status_pending
        ModerationStatus.Rejected -> Res.string.contributions_status_rejected
    },
)

@Composable internal fun ChangeSection.title() = stringResource(
    when (this) {
        ChangeSection.Photos -> Res.string.contributions_section_photos
        ChangeSection.Contacts -> Res.string.contributions_section_contacts
        ChangeSection.Description -> Res.string.contributions_section_description
        ChangeSection.Tags -> Res.string.contributions_section_tags
        ChangeSection.Roasters -> Res.string.contributions_section_roasters
        ChangeSection.Equipment -> Res.string.contributions_section_equipment
        ChangeSection.Menu -> Res.string.contributions_section_menu
        ChangeSection.BrewMethods -> Res.string.contributions_section_brew_methods
    },
)

@Composable internal fun ChangeSection.hint() = stringResource(
    when (this) {
        ChangeSection.Photos -> Res.string.contributions_hint_photos
        ChangeSection.Contacts -> Res.string.contributions_hint_contacts
        ChangeSection.Description -> Res.string.contributions_hint_description
        ChangeSection.Tags -> Res.string.contributions_hint_tags
        ChangeSection.Roasters -> Res.string.contributions_hint_roasters
        ChangeSection.Equipment -> Res.string.contributions_hint_equipment
        ChangeSection.Menu -> Res.string.contributions_hint_menu
        ChangeSection.BrewMethods -> Res.string.contributions_hint_brew_methods
    },
)

/** The one-line summary of a change request; a request without anything to describe shows the hint of its section. */
@Composable internal fun ChangeSummary.text(section: ChangeSection): String = when (this) {
    ChangeSummary.DescriptionCleared -> stringResource(Res.string.contributions_summary_description_cleared)
    ChangeSummary.Contacts -> stringResource(Res.string.contributions_summary_contacts)
    is ChangeSummary.Photos -> stringResource(Res.string.contributions_summary_photos, retained, added)
    is ChangeSummary.Selected -> stringResource(
        when (this.section) {
            ChangeSection.Roasters -> Res.string.contributions_summary_roasters
            ChangeSection.Equipment -> Res.string.contributions_summary_equipment
            ChangeSection.BrewMethods -> Res.string.contributions_summary_brew_methods
            else -> Res.string.contributions_summary_tags
        },
        count,
    )
    is ChangeSummary.Menu -> stringResource(Res.string.contributions_summary_menu, items, newPhotos)
    ChangeSummary.Empty -> section.hint()
}

@Composable internal fun Availability.title() = stringResource(
    when (this) {
        Availability.Present -> Res.string.contributions_availability_present
        Availability.Absent -> Res.string.contributions_availability_absent
        Availability.Unknown -> Res.string.contributions_availability_unknown
    },
)

/** `dd.MM.yyyy HH:mm` in the device's time zone; a value that is not a timestamp is shown as it came (an offset-less one is UTC). */
@OptIn(ExperimentalTime::class)
internal fun localDateTime(iso: String, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = parseServerInstant(iso)?.toLocalDateTime(zone) ?: return iso
    return "${local.dayOfMonth.pad()}.${local.monthNumber.pad()}.${local.year} ${local.hour.pad()}:${local.minute.pad()}"
}

private fun Int.pad() = toString().padStart(2, '0')
