package com.coffeepeek.feature.contributions.domain.rules

import com.coffeepeek.feature.contributions.domain.model.ChangeDraft
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.NewRoaster
import com.coffeepeek.feature.contributions.domain.model.NewShop

object ContributionLimits {
    const val SHOP_NAME = 55
    const val ROASTER_NAME = 100
    const val DESCRIPTION = 1000
    const val SHOP_PHOTOS = 10
    const val MENU_PHOTOS = 4
    const val ROASTER_PHOTOS = 5
}

private val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
private val phonePattern = "^[+]?[\\d\\s()-]{7,20}$".toRegex()
private val urlPattern = "^(https?://)?([a-z0-9-]+\\.)+[a-z]{2,}([/?#].*)?$".toRegex(RegexOption.IGNORE_CASE)
private val handlePattern = "^[A-Za-z0-9._]{2,30}$".toRegex()

enum class ContactIssue { Phone, Email, Website, Instagram }

/** Blank values are fine: every contact is optional. */
fun validateContacts(contacts: Contacts): Set<ContactIssue> = buildSet {
    if (!isValidPhone(contacts.phone.orEmpty())) add(ContactIssue.Phone)
    if (!isValidEmail(contacts.email.orEmpty())) add(ContactIssue.Email)
    if (!isValidWebsite(contacts.website.orEmpty())) add(ContactIssue.Website)
    if (!isValidInstagram(contacts.instagram.orEmpty())) add(ContactIssue.Instagram)
}

fun isValidPhone(value: String): Boolean = value.trim().let { it.isEmpty() || phonePattern.matches(it) }
fun isValidEmail(value: String): Boolean = value.trim().let { it.isEmpty() || emailPattern.matches(it) }
fun isValidWebsite(value: String): Boolean = value.trim().let { it.isEmpty() || urlPattern.matches(it) }

/** A handle (`@name`) or a link: the legacy contacts editor accepted handles only, which rejected the links the server returns. */
fun isValidInstagram(value: String): Boolean {
    val text = value.trim()
    if (text.isEmpty()) return true
    return if (text.startsWith("http", ignoreCase = true)) urlPattern.matches(text) else handlePattern.matches(text.removePrefix("@"))
}

/** The link the roaster form sends: a link stays as typed, a handle becomes `https://instagram.com/<handle>`. */
fun instagramLinkOf(value: String): String? {
    val text = value.trim()
    return when {
        text.isEmpty() -> null
        text.startsWith("http://") || text.startsWith("https://") -> text
        else -> "https://instagram.com/${text.removePrefix("@")}"
    }
}

enum class ShopBasicsIssue { NameBlank, NameTooLong, AddressBlank, CityMissing }

fun validateShopBasics(name: String, address: String, cityId: String?): List<ShopBasicsIssue> = buildList {
    if (name.isBlank()) add(ShopBasicsIssue.NameBlank) else if (name.length > ContributionLimits.SHOP_NAME) add(ShopBasicsIssue.NameTooLong)
    if (address.isBlank()) add(ShopBasicsIssue.AddressBlank)
    if (cityId.isNullOrBlank()) add(ShopBasicsIssue.CityMissing)
}

enum class RoasterIssue { NameBlank, NameTooLong, CityMissing, AddressMissing, Instagram, Website }

/** The city and the address of a roaster come together: one without the other is refused. */
fun validateRoaster(name: String, cityId: String?, address: String, instagram: String, website: String): Set<RoasterIssue> = buildSet {
    if (name.isBlank()) add(RoasterIssue.NameBlank) else if (name.trim().length > ContributionLimits.ROASTER_NAME) add(RoasterIssue.NameTooLong)
    if (address.isNotBlank() && cityId.isNullOrBlank()) add(RoasterIssue.CityMissing)
    if (address.isBlank() && !cityId.isNullOrBlank()) add(RoasterIssue.AddressMissing)
    if (!isValidInstagram(instagram)) add(RoasterIssue.Instagram)
    if (!isValidWebsite(website)) add(RoasterIssue.Website)
}

enum class DraftIssue { DescriptionTooLong, InvalidContacts, TooManyPhotos, InvalidMenuRevision }

/** The reason a request must not be sent, or null. The repository refuses such drafts before any request. */
fun ChangeDraft.issue(): DraftIssue? = when (this) {
    is ChangeDraft.Description -> DraftIssue.DescriptionTooLong.takeIf { text.length > ContributionLimits.DESCRIPTION }
    is ChangeDraft.ContactsChange -> DraftIssue.InvalidContacts.takeIf { validateContacts(contacts).isNotEmpty() }
    is ChangeDraft.Photos -> DraftIssue.TooManyPhotos.takeIf { retainedIds.size + newPhotos.size + uploaded.size > ContributionLimits.SHOP_PHOTOS }
    is ChangeDraft.Menu -> when {
        expectedInputRevision == null || expectedInputRevision < 0 -> DraftIssue.InvalidMenuRevision
        retainedPhotoIds.size + newPhotos.size + uploaded.size > ContributionLimits.MENU_PHOTOS -> DraftIssue.TooManyPhotos
        else -> null
    }
    is ChangeDraft.Selection -> null
}

/** True when the shop passes the same checks as the wizard. */
fun NewShop.isSendable(): Boolean =
    validateShopBasics(name, address, cityId).isEmpty() && validateContacts(contacts).isEmpty() &&
        photos.size <= ContributionLimits.SHOP_PHOTOS && menuPhotos.size <= ContributionLimits.MENU_PHOTOS

fun NewRoaster.isSendable(): Boolean =
    validateRoaster(name, cityId, address.orEmpty(), instagramLink.orEmpty(), siteLink.orEmpty()).isEmpty() &&
        photos.size <= ContributionLimits.ROASTER_PHOTOS
