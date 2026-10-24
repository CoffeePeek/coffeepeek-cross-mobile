package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.domain.model.ChangePayload
import com.coffeepeek.feature.contributions.domain.model.ChangeSection

/** What a change request sends, in numbers and flags; the screen turns it into text. */
internal sealed interface ChangeSummary {
    /** The description was cleared. */
    data object DescriptionCleared : ChangeSummary
    data object Contacts : ChangeSummary
    data class Photos(val retained: Int, val added: Int) : ChangeSummary
    data class Selected(val section: ChangeSection, val count: Int) : ChangeSummary
    data class Menu(val items: Int, val newPhotos: Int) : ChangeSummary

    /** Nothing to describe: the screen shows the hint of the section. */
    data object Empty : ChangeSummary
}

internal fun ChangePayload.summary(): ChangeSummary = when {
    description != null -> if (description!!.isBlank()) ChangeSummary.DescriptionCleared else ChangeSummary.Empty
    contacts != null -> ChangeSummary.Contacts
    photos != null -> ChangeSummary.Photos(photos!!.retainedIds.size, photos!!.uploaded.size)
    tagIds != null -> ChangeSummary.Selected(ChangeSection.Tags, tagIds!!.size)
    roasterIds != null -> ChangeSummary.Selected(ChangeSection.Roasters, roasterIds!!.size)
    equipmentIds != null -> ChangeSummary.Selected(ChangeSection.Equipment, equipmentIds!!.size)
    brewMethodIds != null -> ChangeSummary.Selected(ChangeSection.BrewMethods, brewMethodIds!!.size)
    menu != null -> ChangeSummary.Menu(menu!!.items.size, menu!!.uploaded.size)
    else -> ChangeSummary.Empty
}
