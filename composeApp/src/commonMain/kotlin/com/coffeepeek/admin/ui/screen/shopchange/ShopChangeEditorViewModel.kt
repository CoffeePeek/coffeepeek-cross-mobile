package com.coffeepeek.admin.ui.screen.shopchange

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.utils.MAX_MENU_PHOTOS
import com.coffeepeek.admin.utils.MAX_SHOP_PHOTOS
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.validateOptionalEmail
import com.coffeepeek.admin.utils.validateOptionalInstagram
import com.coffeepeek.admin.utils.validateOptionalPhone
import com.coffeepeek.admin.utils.validateOptionalUrl
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeDrinkDefinition
import com.coffeepeek.domain.model.MenuItemAvailability
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.ShopChangeContacts
import com.coffeepeek.domain.model.ShopChangeDraft
import com.coffeepeek.domain.model.ShopChangeMenuItem
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.domain.model.ShopChangeRequest
import com.coffeepeek.domain.model.ShopPhoto
import com.coffeepeek.domain.model.UploadedPhotoMeta
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopChangeMenuRow(
    val slug: String,
    val name: String,
    val availability: MenuItemAvailability,
    val priceText: String,
)

data class ShopChangeEditorUiState(
    val section: ShopChangeSection,
    val shopTitle: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val submittedRequest: ShopChangeRequest? = null,
    val requestStatus: ModerationStatus? = null,
    val error: String? = null,
    val description: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val instagram: String = "",
    val retainedPhotos: List<ShopPhoto> = emptyList(),
    val newPhotos: List<PickedImage> = emptyList(),
    val alreadyUploadedPhotos: List<UploadedPhotoMeta> = emptyList(),
    val catalogTags: List<CatalogItem> = emptyList(),
    val catalogRoasters: List<CatalogItem> = emptyList(),
    val catalogEquipment: List<CatalogItem> = emptyList(),
    val catalogBrewMethods: List<CatalogItem> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val selectedRoasterIds: Set<String> = emptySet(),
    val selectedEquipmentIds: Set<String> = emptySet(),
    val selectedBrewMethodIds: Set<String> = emptySet(),
    val menuRows: List<ShopChangeMenuRow> = emptyList(),
    val retainedMenuPhotos: List<ShopPhoto> = emptyList(),
    val newMenuPhotos: List<PickedImage> = emptyList(),
    val alreadyUploadedMenuPhotos: List<UploadedPhotoMeta> = emptyList(),
) {
    val descriptionError: String? get() = if (description.length > 1000) "Не более 1000 символов" else null
    val phoneError: String? get() = validateOptionalPhone(phone)
    val emailError: String? get() = validateOptionalEmail(email)
    val websiteError: String? get() = validateOptionalUrl(website)
    val instagramError: String? get() = validateOptionalInstagram(instagram)
    val contactsValid: Boolean get() =
        phoneError == null && emailError == null && websiteError == null && instagramError == null
    val photoCount: Int get() = retainedPhotos.size + newPhotos.size + alreadyUploadedPhotos.size
    val menuPhotoCount: Int get() = retainedMenuPhotos.size + newMenuPhotos.size + alreadyUploadedMenuPhotos.size
    val canSubmit: Boolean get() = !isLoading && !isSubmitting && submittedRequest == null && when (section) {
        ShopChangeSection.Description -> descriptionError == null
        ShopChangeSection.Contacts -> contactsValid
        ShopChangeSection.Photos -> photoCount <= MAX_SHOP_PHOTOS
        ShopChangeSection.Menu -> menuPhotoCount <= MAX_MENU_PHOTOS
        else -> true
    }
}

class ShopChangeEditorViewModel(
    private val shopId: String,
    private val section: ShopChangeSection,
    requestIdRaw: String,
    private val shopRepository: ShopRepository,
    private val changeRepository: ShopChangeRequestRepository,
) : BaseViewModel() {

    private val requestId = requestIdRaw.ifBlank { null }

    private val _state = MutableStateFlow(ShopChangeEditorUiState(section = section))
    val state = _state.asStateFlow()

    init {
        workScope.launch { load() }
    }

    private suspend fun load() {
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
            )
        }
        val catalogs = shopRepository.getCatalogs().getOrElse { e ->
            _state.update { it.copy(isLoading = false, error = e.message) }
            return
        }
        val details = shopRepository.getShopDetails(shopId).getOrElse { e ->
            _state.update { it.copy(isLoading = false, error = e.message) }
            return
        }
        val drinks = shopRepository.getMenuDrinks().getOrNull().orEmpty()
        _state.update { current ->
            current.copy(
                shopTitle = details.shop.title,
                catalogTags = catalogs.shopTags,
                catalogRoasters = catalogs.roasters,
                catalogEquipment = catalogs.equipment,
                catalogBrewMethods = catalogs.brewMethods,
                description = details.description.orEmpty(),
                phone = details.contact?.phone.orEmpty(),
                email = details.contact?.email.orEmpty(),
                website = details.contact?.website.orEmpty(),
                instagram = details.contact?.instagram.orEmpty(),
                retainedPhotos = details.shopPhotos,
                selectedTagIds = details.tagItems.map { it.id }.toSet(),
                selectedRoasterIds = details.roasters.map { it.id }.toSet(),
                selectedEquipmentIds = details.equipmentItems.map { it.id }.toSet(),
                selectedBrewMethodIds = details.brewMethodItems.map { it.id }.toSet(),
                menuRows = drinks.toMenuRows(details.menu?.items.orEmpty()),
                retainedMenuPhotos = details.menu?.photos.orEmpty().map {
                    ShopPhoto(id = it.id, fullUrl = it.fullUrl, previewUrl = it.previewUrl, sortIndex = it.sortIndex)
                },
            )
        }
        if (!requestId.isNullOrBlank()) {
            changeRepository.getById(requestId)
                .onSuccess { request ->
                    _state.update { it.applyRequest(request, drinks, details.shopPhotos, details.menu?.photos.orEmpty()) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
        _state.update { it.copy(isLoading = false) }
    }

    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value.take(1000), error = null) }
    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value, error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onWebsiteChange(value: String) = _state.update { it.copy(website = value, error = null) }
    fun onInstagramChange(value: String) = _state.update { it.copy(instagram = value, error = null) }

    fun toggleTag(id: String) = _state.update { state ->
        state.copy(selectedTagIds = state.selectedTagIds.toggle(id))
    }

    fun toggleRoaster(id: String) = _state.update { state ->
        state.copy(selectedRoasterIds = state.selectedRoasterIds.toggle(id))
    }

    fun toggleEquipment(id: String) = _state.update { state ->
        state.copy(selectedEquipmentIds = state.selectedEquipmentIds.toggle(id))
    }

    fun toggleBrewMethod(id: String) = _state.update { state ->
        state.copy(selectedBrewMethodIds = state.selectedBrewMethodIds.toggle(id))
    }

    fun addPhotos(images: List<PickedImage>) = _state.update { state ->
        val remaining = MAX_SHOP_PHOTOS - state.photoCount
        state.copy(newPhotos = state.newPhotos + images.take(remaining.coerceAtLeast(0)))
    }

    fun removeRetainedPhoto(id: String) = _state.update { state ->
        state.copy(retainedPhotos = state.retainedPhotos.filterNot { it.id == id })
    }

    fun removeNewPhoto(index: Int) = _state.update { state ->
        state.copy(newPhotos = state.newPhotos.filterIndexed { i, _ -> i != index })
    }

    fun addMenuPhotos(images: List<PickedImage>) = _state.update { state ->
        val remaining = MAX_MENU_PHOTOS - state.menuPhotoCount
        state.copy(newMenuPhotos = state.newMenuPhotos + images.take(remaining.coerceAtLeast(0)))
    }

    fun removeRetainedMenuPhoto(id: String) = _state.update { state ->
        state.copy(retainedMenuPhotos = state.retainedMenuPhotos.filterNot { it.id == id })
    }

    fun removeNewMenuPhoto(index: Int) = _state.update { state ->
        state.copy(newMenuPhotos = state.newMenuPhotos.filterIndexed { i, _ -> i != index })
    }

    fun updateMenuRow(slug: String, transform: (ShopChangeMenuRow) -> ShopChangeMenuRow) {
        _state.update { state ->
            state.copy(menuRows = state.menuRows.map { row -> if (row.slug == slug) transform(row) else row })
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearSubmissionError() = _state.update { it.copy(submissionError = null) }

    fun submit() {
        val snapshot = _state.value
        if (!snapshot.canSubmit || snapshot.isSubmitting) return
        if (!_state.compareAndSet(snapshot, snapshot.copy(isSubmitting = true, submissionError = null))) return
        workScope.launch {
            val draft = snapshot.toDraft(shopId, section)
            val result = try { if (requestId.isNullOrBlank()) {
                changeRepository.create(draft)
            } else {
                changeRepository.update(requestId, draft)
            } } catch (cancelled: CancellationException) {
                _state.update { it.copy(isSubmitting = false) }
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            result
                .onSuccess { request ->
                    _state.update { it.copy(isSubmitting = false, submittedRequest = request) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmitting = false, submissionError = e.message?.takeIf { it.isNotBlank() } ?: "Не удалось отправить заявку") }
                }
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> =
    if (id in this) this - id else this + id

private fun List<CoffeeDrinkDefinition>.toMenuRows(
    current: List<com.coffeepeek.domain.model.ShopMenuItem>,
): List<ShopChangeMenuRow> {
    val bySlug = current.associateBy { it.slug.lowercase() }
    return map { drink ->
        val item = bySlug[drink.slug.lowercase()]
        ShopChangeMenuRow(
            slug = drink.slug,
            name = drink.nameRu.ifBlank { drink.nameEn }.ifBlank { drink.slug },
            availability = item?.availability.toAvailability(),
            priceText = item?.price?.let { formatNumber(it) }.orEmpty(),
        )
    }
}

private fun String?.toAvailability(): MenuItemAvailability = when (this?.lowercase()) {
    "present", "1" -> MenuItemAvailability.Present
    "absent", "2" -> MenuItemAvailability.Absent
    else -> MenuItemAvailability.Unknown
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun ShopChangeEditorUiState.applyRequest(
    request: com.coffeepeek.domain.model.ShopChangeRequest,
    drinks: List<CoffeeDrinkDefinition>,
    shopPhotos: List<ShopPhoto>,
    menuPhotos: List<com.coffeepeek.domain.model.ShopMenuPhoto>,
): ShopChangeEditorUiState {
    val payload = request.payload
    val photosById = shopPhotos.associateBy { it.id }
    val menuPhotosById = menuPhotos.associateBy { it.id }
    return copy(
        requestStatus = request.status,
        description = payload.description ?: description,
        phone = payload.contacts?.phoneNumber ?: phone,
        email = payload.contacts?.email ?: email,
        website = payload.contacts?.siteLink ?: website,
        instagram = payload.contacts?.instagramLink ?: instagram,
        retainedPhotos = payload.photos?.retainedPhotoIds?.mapNotNull { photosById[it] } ?: retainedPhotos,
        alreadyUploadedPhotos = payload.photos?.newPhotos.orEmpty(),
        selectedTagIds = payload.tagIds?.toSet() ?: selectedTagIds,
        selectedRoasterIds = payload.roasterIds?.toSet() ?: selectedRoasterIds,
        selectedEquipmentIds = payload.equipmentIds?.toSet() ?: selectedEquipmentIds,
        selectedBrewMethodIds = payload.brewMethodIds?.toSet() ?: selectedBrewMethodIds,
        retainedMenuPhotos = payload.menu?.retainedPhotoIds?.mapNotNull { id ->
            menuPhotosById[id]?.let { ShopPhoto(it.id, it.fullUrl, it.previewUrl, it.sortIndex) }
        } ?: retainedMenuPhotos,
        alreadyUploadedMenuPhotos = payload.menu?.newPhotos.orEmpty(),
        menuRows = payload.menu?.items?.let { items ->
            val bySlug = items.associateBy { it.slug.lowercase() }
            drinks.map { drink ->
                val item = bySlug[drink.slug.lowercase()]
                ShopChangeMenuRow(
                    slug = drink.slug,
                    name = drink.nameRu.ifBlank { drink.nameEn }.ifBlank { drink.slug },
                    availability = item?.availability ?: MenuItemAvailability.Unknown,
                    priceText = item?.price?.let { formatNumber(it) }.orEmpty(),
                )
            }
        } ?: menuRows,
    )
}

private fun ShopChangeEditorUiState.toDraft(shopId: String, section: ShopChangeSection) = ShopChangeDraft(
    shopId = shopId,
    section = section,
    description = description.trim().ifBlank { null },
    contacts = ShopChangeContacts(
        phoneNumber = phone.trim().ifBlank { null },
        email = email.trim().ifBlank { null },
        siteLink = website.trim().ifBlank { null },
        instagramLink = instagram.trim().ifBlank { null },
    ),
    retainedPhotoIds = retainedPhotos.map { it.id },
    newPhotos = newPhotos.map { it.toPending() },
    alreadyUploadedPhotos = alreadyUploadedPhotos,
    tagIds = selectedTagIds.toList(),
    roasterIds = selectedRoasterIds.toList(),
    equipmentIds = selectedEquipmentIds.toList(),
    brewMethodIds = selectedBrewMethodIds.toList(),
    menuItems = menuRows.map { row ->
        ShopChangeMenuItem(
            slug = row.slug,
            availability = row.availability,
            price = row.priceText.replace(',', '.').toDoubleOrNull(),
        )
    },
    retainedMenuPhotoIds = retainedMenuPhotos.map { it.id },
    newMenuPhotos = newMenuPhotos.map { it.toPending() },
    alreadyUploadedMenuPhotos = alreadyUploadedMenuPhotos,
)

private fun PickedImage.toPending() = PendingPhotoUpload(
    fileName = fileName,
    contentType = contentType,
    bytes = bytes,
)
