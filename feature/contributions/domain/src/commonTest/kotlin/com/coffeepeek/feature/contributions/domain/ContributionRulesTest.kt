package com.coffeepeek.feature.contributions.domain

import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.rules.*
import kotlin.test.*

class ContributionRulesTest {
    @Test fun contactsAreOptionalAndEachInvalidOneIsReportedOnItsOwn() {
        assertEquals(emptySet(), validateContacts(Contacts()))
        assertEquals(emptySet(), validateContacts(Contacts(phone = " ", email = "", website = "  ", instagram = null)))
        assertEquals(emptySet(), validateContacts(Contacts("+375 (29) 111-22-33", "shop@example.com", "coffeepeek.by", "@shop.cafe")))
        assertEquals(
            setOf(ContactIssue.Phone, ContactIssue.Email, ContactIssue.Website, ContactIssue.Instagram),
            validateContacts(Contacts("abc", "not-an-email", "bad url", "a")),
        )
    }

    @Test fun instagramAcceptsAHandleOrALinkButNotAnotherScheme() {
        assertTrue(isValidInstagram("shop_cafe"))
        assertTrue(isValidInstagram("@shop.cafe"))
        assertTrue(isValidInstagram("https://instagram.com/shop_cafe"))
        assertTrue(isValidInstagram("HTTP://instagram.com/shop"))
        assertFalse(isValidInstagram("a"))
        assertFalse(isValidInstagram("two words"))
        assertFalse(isValidInstagram("http//broken"))
    }

    @Test fun roasterInstagramBecomesALinkAndKeepsAnExistingOne() {
        assertEquals("https://instagram.com/roast.shop", instagramLinkOf("@roast.shop"))
        assertEquals("https://instagram.com/roast", instagramLinkOf(" roast "))
        assertEquals("https://instagram.com/roast", instagramLinkOf("https://instagram.com/roast"))
        assertNull(instagramLinkOf("  "))
    }

    @Test fun shopBasicsNeedANameWithinTheLimitAnAddressAndACity() {
        assertEquals(emptyList(), validateShopBasics("Кофейня", "Улица, 1", "minsk"))
        assertEquals(listOf(ShopBasicsIssue.NameBlank, ShopBasicsIssue.AddressBlank, ShopBasicsIssue.CityMissing), validateShopBasics(" ", "", null))
        assertEquals(listOf(ShopBasicsIssue.NameTooLong), validateShopBasics("x".repeat(ContributionLimits.SHOP_NAME + 1), "Улица", "minsk"))
        assertEquals(emptyList(), validateShopBasics("x".repeat(ContributionLimits.SHOP_NAME), "Улица", "minsk"))
        assertEquals(listOf(ShopBasicsIssue.CityMissing), validateShopBasics("Кофейня", "Улица", " "))
    }

    @Test fun roasterCityAndAddressComeTogether() {
        assertEquals(emptySet(), validateRoaster("Roast", null, "", "", ""))
        assertEquals(emptySet(), validateRoaster("Roast", "minsk", "Улица", "@roast", "roast.example"))
        assertEquals(setOf(RoasterIssue.CityMissing), validateRoaster("Roast", null, "Улица", "", ""))
        assertEquals(setOf(RoasterIssue.AddressMissing), validateRoaster("Roast", "minsk", " ", "", ""))
        assertEquals(setOf(RoasterIssue.NameBlank, RoasterIssue.Instagram, RoasterIssue.Website), validateRoaster("", null, "", "a", "bad url"))
        assertEquals(setOf(RoasterIssue.NameTooLong), validateRoaster("n".repeat(ContributionLimits.ROASTER_NAME + 1), null, "", "", ""))
    }

    @Test fun aDraftIsRefusedForADescriptionContactsOrPhotosOverTheLimit() {
        assertNull(ChangeDraft.Description("shop", "x".repeat(ContributionLimits.DESCRIPTION)).issue())
        assertEquals(DraftIssue.DescriptionTooLong, ChangeDraft.Description("shop", "x".repeat(ContributionLimits.DESCRIPTION + 1)).issue())
        assertEquals(DraftIssue.InvalidContacts, ChangeDraft.ContactsChange("shop", Contacts(email = "nope")).issue())
        assertNull(ChangeDraft.ContactsChange("shop", Contacts(instagram = "https://instagram.com/shop")).issue())
        fun photo(n: Int) = PendingPhoto("p$n.jpg", "image/jpeg", byteArrayOf(1))
        val earlier = UploadedPhoto("e.jpg", "image/jpeg", "key", 1)
        assertNull(ChangeDraft.Photos("shop", List(8) { "r$it" }, listOf(photo(1)), listOf(earlier)).issue())
        assertEquals(DraftIssue.TooManyPhotos, ChangeDraft.Photos("shop", List(8) { "r$it" }, listOf(photo(1), photo(2)), listOf(earlier)).issue())
        assertNull(ChangeDraft.Menu("shop", emptyList(), listOf("m"), listOf(photo(1), photo(2)), listOf(earlier)).issue())
        assertEquals(DraftIssue.TooManyPhotos, ChangeDraft.Menu("shop", emptyList(), listOf("m", "n"), listOf(photo(1), photo(2)), listOf(earlier)).issue())
        assertNull(ChangeDraft.Selection("shop", ChangeSection.Tags, listOf("a")).issue())
    }

    @Test fun aSelectionDraftOnlyExistsForCatalogSections() {
        assertFailsWith<IllegalArgumentException> { ChangeDraft.Selection("shop", ChangeSection.Menu, emptyList()) }
        assertEquals(ChangeSection.Equipment, ChangeDraft.Selection("shop", ChangeSection.Equipment, emptyList()).section)
    }

    @Test fun shopsAndRoastersAreSendableOnlyWithTheWizardRules() {
        val shop = NewShop("Кофейня", "Улица, 1", "minsk")
        assertTrue(shop.isSendable())
        assertFalse(shop.copy(cityId = "").isSendable())
        assertFalse(shop.copy(contacts = Contacts(phone = "abc")).isSendable())
        assertFalse(shop.copy(menuPhotos = List(5) { PendingPhoto("m.jpg", "image/jpeg", byteArrayOf(1)) }).isSendable())
        assertTrue(NewRoaster("Roast").isSendable())
        assertFalse(NewRoaster("Roast", cityId = "minsk").isSendable())
        assertFalse(NewRoaster("Roast", photos = List(6) { PendingPhoto("p.jpg", "image/jpeg", byteArrayOf(1)) }).isSendable())
    }

    @Test fun pagedKnowsWhetherThereIsAnotherPage() {
        assertTrue(Paged(listOf(1), 3, 3, 1).hasMore)
        assertFalse(Paged(listOf(1), 3, 3, 3).hasMore)
        assertFalse(Paged(emptyList<Int>(), 0, 0, 1).hasMore)
    }

    @Test fun pendingPhotosCompareByTheirBytes() {
        assertEquals(PendingPhoto("a.jpg", "image/jpeg", byteArrayOf(1, 2)), PendingPhoto("a.jpg", "image/jpeg", byteArrayOf(1, 2)))
        assertNotEquals(PendingPhoto("a.jpg", "image/jpeg", byteArrayOf(1, 2)), PendingPhoto("a.jpg", "image/jpeg", byteArrayOf(1, 3)))
    }
}
