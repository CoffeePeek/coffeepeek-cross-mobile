package com.coffeepeek.feature.ownership.domain

import com.coffeepeek.feature.ownership.domain.model.*
import kotlin.test.*

class OwnershipRulesTest {
    @Test fun publicLinksAreConvertedToSlugsWithoutFetchingThem() {
        val input = ClaimSubmission(BusinessKind.CoffeeShop, " https://coffeepeek.by/coffee-shops/example?from=share ", " contact ", " evidence ").validated()
        assertEquals("example", input.slug)
        assertEquals("contact", input.contact)
        assertFailsWith<OwnershipFailure> { input.copy(slug = "https://foreign.example/coffee-shops/example").validated() }
        assertFailsWith<OwnershipFailure> { input.copy(slug = "https://coffeepeek.by/roasters/example").validated() }
    }
    @Test fun roasterLinksAndWhitespaceLimitsMatchTheContract() {
        val input = ClaimSubmission(BusinessKind.Roaster, "https://coffeepeek.by/roasters/roast/", "c", "e").validated()
        assertEquals("roast", input.slug)
        assertFailsWith<OwnershipFailure> { input.copy(contact = " ").validated() }
        assertFailsWith<OwnershipFailure> { input.copy(contact = "c".repeat(321)).validated() }
        assertFailsWith<OwnershipFailure> { input.copy(evidence = "e".repeat(4001)).validated() }
    }
    @Test fun profileDraftRetainsIdentityRevisionAndUntouchedLocation() {
        val original = BusinessProfile("business", BusinessKind.Roaster, 8, "Name", cityId = "city-id", latitude = 53.9, longitude = 27.5)
        val edited = ProfileDraft.from(original).copy(about = "Edited").applyTo(original)
        assertEquals("business", edited.id)
        assertEquals(8, edited.revision)
        assertEquals("city-id", edited.cityId)
        assertEquals(original.latitude, edited.latitude)
    }
    @Test fun invalidCoordinatesAndBlankNamesCannotBeSaved() {
        assertFailsWith<OwnershipFailure> { ProfileDraft(name = " ").validate() }
        assertFailsWith<OwnershipFailure> { ProfileDraft(name = "Name", latitude = "NaN", longitude = "10").validate() }
        assertFailsWith<OwnershipFailure> { ProfileDraft(name = "Name", latitude = "91", longitude = "10").validate() }
        assertFailsWith<OwnershipFailure> { ProfileDraft(name = "Name", latitude = "53").validate() }
    }
    @Test fun unsafeLinksAndOversizedGalleriesCannotBeSaved() {
        assertFailsWith<OwnershipFailure> { ProfileDraft(name = "Name", site = "javascript:alert(1)").validate() }
        val photos = List(11) { BusinessPhoto(it.toString(), fileName = "photo.jpg") }
        assertFailsWith<OwnershipFailure> { ProfileDraft(name = "Name", photos = photos).validate() }
    }
}
