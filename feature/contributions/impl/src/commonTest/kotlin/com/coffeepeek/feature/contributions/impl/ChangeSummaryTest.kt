package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangeSummaryTest {
    private val uploaded = UploadedPhoto("n.jpg", "image/jpeg", "key", 1)

    @Test fun everySectionIsDescribedByItsOwnNumbers() {
        assertEquals(ChangeSummary.DescriptionCleared, ChangePayload(description = " ").summary())
        assertEquals(ChangeSummary.Empty, ChangePayload(description = "Текст").summary())
        assertEquals(ChangeSummary.Contacts, ChangePayload(contacts = Contacts(email = "a@b.c")).summary())
        assertEquals(ChangeSummary.Photos(2, 1), ChangePayload(photos = ChangePhotos(listOf("1", "2"), listOf(uploaded))).summary())
        assertEquals(ChangeSummary.Selected(ChangeSection.Tags, 3), ChangePayload(tagIds = listOf("a", "b", "c")).summary())
        assertEquals(ChangeSummary.Selected(ChangeSection.Roasters, 1), ChangePayload(roasterIds = listOf("a")).summary())
        assertEquals(ChangeSummary.Selected(ChangeSection.Equipment, 2), ChangePayload(equipmentIds = listOf("a", "b")).summary())
        assertEquals(ChangeSummary.Selected(ChangeSection.BrewMethods, 0), ChangePayload(brewMethodIds = emptyList()).summary())
        assertEquals(ChangeSummary.Menu(1, 1), ChangePayload(menu = ChangeMenu(listOf(MenuEntry("x", Availability.Present)), uploaded = listOf(uploaded))).summary())
        assertEquals(ChangeSummary.Empty, ChangePayload().summary())
    }
}
