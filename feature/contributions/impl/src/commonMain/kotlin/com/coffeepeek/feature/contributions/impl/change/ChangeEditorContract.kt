package com.coffeepeek.feature.contributions.impl.change

import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.ChangeDraft
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.MenuEntry
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.model.ShopPhotoRef
import com.coffeepeek.feature.contributions.domain.model.UploadedPhoto
import com.coffeepeek.feature.contributions.domain.rules.ContactIssue
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.issue
import com.coffeepeek.feature.contributions.domain.rules.validateContacts

internal enum class EditorLoadError { SignInRequired, NotFound, Failed }

internal sealed interface EditorSubmit {
    data object Idle : EditorSubmit
    data object Sending : EditorSubmit
    data class Done(val request: ChangeRequest) : EditorSubmit

    /** [serverMessage] is the server's own wording; null shows the default text. */
    data class Failed(val serverMessage: String?) : EditorSubmit
}

internal data class MenuRowState(val slug: String, val name: String, val availability: Availability = Availability.Unknown, val priceText: String = "")

internal data class ChangeEditorState(
    val section: ChangeSection,
    /** Null until the first answer about the account; a guest cannot send a change. */
    val signedIn: Boolean? = null,
    val isLoading: Boolean = true,
    val loadError: EditorLoadError? = null,
    val loadMessage: String? = null,
    val shopTitle: String = "",
    val catalogs: Catalogs = Catalogs(),
    val description: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val instagram: String = "",
    val retainedPhotos: List<ShopPhotoRef> = emptyList(),
    val newPhotos: List<PendingPhoto> = emptyList(),
    /** Photos of the earlier request that are already stored; they are sent again without a new upload. */
    val uploadedPhotos: List<UploadedPhoto> = emptyList(),
    val tagIds: Set<String> = emptySet(),
    val roasterIds: Set<String> = emptySet(),
    val equipmentIds: Set<String> = emptySet(),
    val brewMethodIds: Set<String> = emptySet(),
    val menuRows: List<MenuRowState> = emptyList(),
    val retainedMenuPhotos: List<ShopPhotoRef> = emptyList(),
    val newMenuPhotos: List<PendingPhoto> = emptyList(),
    val uploadedMenuPhotos: List<UploadedPhoto> = emptyList(),
    val submit: EditorSubmit = EditorSubmit.Idle,
) {
    val contacts: Contacts get() = Contacts(phone.trimmed(), email.trimmed(), website.trimmed(), instagram.trimmed())
    val contactIssues: Set<ContactIssue> get() = validateContacts(contacts)
    val descriptionTooLong: Boolean get() = description.length > ContributionLimits.DESCRIPTION
    val photoCount: Int get() = retainedPhotos.size + newPhotos.size + uploadedPhotos.size
    val menuPhotoCount: Int get() = retainedMenuPhotos.size + newMenuPhotos.size + uploadedMenuPhotos.size
    val busy: Boolean get() = submit == EditorSubmit.Sending || submit is EditorSubmit.Done
    val canSubmit: Boolean get() = signedIn == true && !isLoading && loadError == null && !busy && toDraft("").issue() == null

    /** What is sent for [section] only, for the shop [slug]; another section's fields never travel with it. */
    fun toDraft(slug: String): ChangeDraft = when (section) {
        ChangeSection.Description -> ChangeDraft.Description(slug, description)
        ChangeSection.Contacts -> ChangeDraft.ContactsChange(slug, contacts)
        ChangeSection.Photos -> ChangeDraft.Photos(slug, retainedPhotos.map { it.id }, newPhotos, uploadedPhotos)
        ChangeSection.Tags -> ChangeDraft.Selection(slug, section, tagIds.toList())
        ChangeSection.Roasters -> ChangeDraft.Selection(slug, section, roasterIds.toList())
        ChangeSection.Equipment -> ChangeDraft.Selection(slug, section, equipmentIds.toList())
        ChangeSection.BrewMethods -> ChangeDraft.Selection(slug, section, brewMethodIds.toList())
        ChangeSection.Menu -> ChangeDraft.Menu(
            slug, menuRows.map { MenuEntry(it.slug, it.availability, it.priceText.replace(',', '.').toDoubleOrNull()) },
            retainedMenuPhotos.map { it.id }, newMenuPhotos, uploadedMenuPhotos,
        )
    }
}

private fun String.trimmed(): String? = trim().takeIf(String::isNotEmpty)

internal sealed interface ChangeEditorAction {
    data class Description(val value: String) : ChangeEditorAction
    data class Phone(val value: String) : ChangeEditorAction
    data class Email(val value: String) : ChangeEditorAction
    data class Website(val value: String) : ChangeEditorAction
    data class Instagram(val value: String) : ChangeEditorAction

    /** Toggles an item of the catalog of the editor's section. */
    data class Toggle(val id: String) : ChangeEditorAction

    data class AddPhotos(val photos: List<PendingPhoto>) : ChangeEditorAction
    data class RemoveRetainedPhoto(val id: String) : ChangeEditorAction
    data class RemoveNewPhoto(val index: Int) : ChangeEditorAction
    data object RemoveUploadedPhotos : ChangeEditorAction
    data class AddMenuPhotos(val photos: List<PendingPhoto>) : ChangeEditorAction
    data class RemoveRetainedMenuPhoto(val id: String) : ChangeEditorAction
    data class RemoveNewMenuPhoto(val index: Int) : ChangeEditorAction
    data object RemoveUploadedMenuPhotos : ChangeEditorAction
    data class SetAvailability(val slug: String, val availability: Availability) : ChangeEditorAction
    data class SetPrice(val slug: String, val text: String) : ChangeEditorAction

    data object Reload : ChangeEditorAction
    data object Submit : ChangeEditorAction
    data object DismissFailure : ChangeEditorAction
}

internal sealed interface ChangeEditorEvent
