package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.validateOptionalInstagram
import com.coffeepeek.admin.utils.validateOptionalUrl
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.CreateRoasterInput
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.RoasterSubmissionResult
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_ROASTER_PHOTOS = 5

data class AddRoasterUiState(
    val name: String = "",
    val about: String = "",
    val cities: List<City> = emptyList(),
    val selectedCity: City? = null,
    val address: String = "",
    val instagram: String = "",
    val website: String = "",
    val photos: List<PickedImage> = emptyList(),
    val isLoadingCatalogs: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val submissionError: String? = null,
    val result: RoasterSubmissionResult? = null,
) {
    val nameError: String?
        get() = when {
            name.isBlank() -> "Введите название"
            name.trim().length > 100 -> "Не более 100 символов"
            else -> null
        }

    val locationError: String?
        get() = when {
            address.isNotBlank() && selectedCity == null -> "Для адреса выберите город"
            address.isBlank() && selectedCity != null -> "Для выбранного города укажите адрес"
            else -> null
        }

    val instagramError: String?
        get() = if (instagram.trim().startsWith("http", ignoreCase = true)) {
            validateOptionalUrl(instagram)
        } else {
            validateOptionalInstagram(instagram)
        }

    val websiteError: String? get() = validateOptionalUrl(website)

    val canSubmit: Boolean
        get() = nameError == null && locationError == null &&
            instagramError == null && websiteError == null && !isLoadingCatalogs && !isSubmitting && result == null
}

class AddRoasterViewModel(
    private val roasterRepository: RoasterRepository,
    private val shopRepository: ShopRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(AddRoasterUiState())
    val state = _state.asStateFlow()

    init {
        workScope.launch {
            shopRepository.getCatalogs()
                .onSuccess { catalogs ->
                    _state.update {
                        it.copy(
                            cities = catalogs.cities,
                            isLoadingCatalogs = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingCatalogs = false,
                            error = error.message ?: "Не удалось загрузить города",
                        )
                    }
                }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(100)) }
    fun onAboutChange(value: String) = _state.update { it.copy(about = value) }
    fun onCitySelect(city: City?) = _state.update { it.copy(selectedCity = city) }
    fun onAddressChange(value: String) = _state.update { it.copy(address = value) }
    fun onInstagramChange(value: String) = _state.update { it.copy(instagram = value) }
    fun onWebsiteChange(value: String) = _state.update { it.copy(website = value) }
    fun clearError() = _state.update { it.copy(error = null) }
    fun clearSubmissionError() = _state.update { it.copy(submissionError = null) }

    fun addPhotos(images: List<PickedImage>) {
        _state.update { state ->
            state.copy(photos = (state.photos + images).take(MAX_ROASTER_PHOTOS))
        }
    }

    fun removePhoto(index: Int) {
        _state.update { state ->
            state.copy(photos = state.photos.filterIndexed { itemIndex, _ -> itemIndex != index })
        }
    }

    fun submit() {
        val snapshot = _state.value
        if (!snapshot.canSubmit) return
        if (!_state.compareAndSet(snapshot, snapshot.copy(isSubmitting = true, submissionError = null))) return
        workScope.launch {
            val result = try { roasterRepository.submitRoaster(
                CreateRoasterInput(
                    name = snapshot.name,
                    about = snapshot.about.takeIf { it.isNotBlank() },
                    cityId = snapshot.selectedCity?.id,
                    address = snapshot.address.takeIf { it.isNotBlank() },
                    instagramLink = normalizeInstagram(snapshot.instagram),
                    siteLink = snapshot.website.takeIf { it.isNotBlank() },
                    photos = snapshot.photos.map {
                        PendingPhotoUpload(it.fileName, it.contentType, it.bytes)
                    },
                ),
            ) } catch (cancelled: CancellationException) {
                _state.update { it.copy(isSubmitting = false) }
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            result.onSuccess { result ->
                _state.update { it.copy(isSubmitting = false, result = result) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submissionError = error.message?.takeIf { it.isNotBlank() } ?: "Не удалось отправить обжарщика на модерацию",
                    )
                }
            }
        }
    }

    private fun normalizeInstagram(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return "https://instagram.com/${trimmed.removePrefix("@")}"
    }
}
