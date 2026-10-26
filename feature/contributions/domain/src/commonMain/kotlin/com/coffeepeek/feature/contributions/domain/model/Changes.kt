package com.coffeepeek.feature.contributions.domain.model

enum class ChangeSection { Photos, Contacts, Description, Tags, Roasters, Equipment, Menu, BrewMethods }

enum class Availability { Unknown, Present, Absent }

data class Contacts(
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val instagram: String? = null,
)

data class MenuEntry(val slug: String, val availability: Availability, val price: Double? = null)

data class ChangePhotos(val retainedIds: List<String> = emptyList(), val uploaded: List<UploadedPhoto> = emptyList())

data class ChangeMenu(
    val items: List<MenuEntry> = emptyList(),
    val retainedPhotoIds: List<String> = emptyList(),
    val uploaded: List<UploadedPhoto> = emptyList(),
)

/** What a request changes. Only the part of its section is set. */
data class ChangePayload(
    val description: String? = null,
    val contacts: Contacts? = null,
    val photos: ChangePhotos? = null,
    val tagIds: List<String>? = null,
    val roasterIds: List<String>? = null,
    val equipmentIds: List<String>? = null,
    val brewMethodIds: List<String>? = null,
    val menu: ChangeMenu? = null,
)

data class ChangeRequest(
    val id: String,
    val shopSlug: String,
    val section: ChangeSection,
    val payload: ChangePayload,
    val status: ModerationStatus,
    val rejectionReason: String?,
    val createdAtUtc: String,
    val updatedAtUtc: String?,
)

/** What the user sends for one section of one shop; each section has its own shape, so a draft cannot carry another section's data. */
sealed interface ChangeDraft {
    val shopSlug: String
    val section: ChangeSection

    data class Description(override val shopSlug: String, val text: String) : ChangeDraft {
        override val section get() = ChangeSection.Description
    }

    data class ContactsChange(override val shopSlug: String, val contacts: Contacts) : ChangeDraft {
        override val section get() = ChangeSection.Contacts
    }

    /** [uploaded] are photos of an earlier request of the same user that are sent again without a new upload. */
    data class Photos(
        override val shopSlug: String,
        val retainedIds: List<String>,
        val newPhotos: List<PendingPhoto>,
        val uploaded: List<UploadedPhoto> = emptyList(),
    ) : ChangeDraft {
        override val section get() = ChangeSection.Photos
    }

    /** The chosen catalog items of [section]: tags, roasters, equipment or brew methods. */
    data class Selection(override val shopSlug: String, override val section: ChangeSection, val ids: List<String>) : ChangeDraft {
        init { require(section in selectionSections) { "Not a catalog section: $section" } }
    }

    data class Menu(
        override val shopSlug: String,
        val items: List<MenuEntry>,
        val retainedPhotoIds: List<String>,
        val newPhotos: List<PendingPhoto>,
        val uploaded: List<UploadedPhoto> = emptyList(),
    ) : ChangeDraft {
        override val section get() = ChangeSection.Menu
    }
}

private val selectionSections = setOf(ChangeSection.Tags, ChangeSection.Roasters, ChangeSection.Equipment, ChangeSection.BrewMethods)
