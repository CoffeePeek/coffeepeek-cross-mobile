package com.coffeepeek.feature.contributions.impl.shop

import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.model.PriceRange
import com.coffeepeek.feature.contributions.domain.rules.ContactIssue
import com.coffeepeek.feature.contributions.domain.rules.DayInput
import com.coffeepeek.feature.contributions.domain.rules.ShopBasicsIssue
import com.coffeepeek.feature.contributions.domain.rules.WeekOrder
import com.coffeepeek.feature.contributions.domain.rules.validateContacts
import com.coffeepeek.feature.contributions.domain.rules.validateShopBasics

internal enum class AddShopStep { Basic, Contacts, Photos, Features, Schedule }

internal enum class LocationHint { NotFound, NoAddress, PermissionDenied }

internal sealed interface SubmitState {
    data object Idle : SubmitState
    data object Sending : SubmitState
    data object Done : SubmitState

    /** [serverMessage] is the server's own wording; null shows the default text. */
    data class Failed(val serverMessage: String?) : SubmitState
}

internal const val DEFAULT_OPEN = "09:00"
internal const val DEFAULT_CLOSE = "21:00"

internal data class AddShopState(
    /** Null until the first answer about the account; a guest cannot send a shop. */
    val signedIn: Boolean? = null,
    val catalogs: Catalogs = Catalogs(),
    val catalogsLoading: Boolean = true,
    val catalogsFailed: Boolean = false,
    val name: String = "",
    val cityId: String? = null,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String = "",
    val priceRange: PriceRange? = null,
    val photos: List<PendingPhoto> = emptyList(),
    val menuPhotos: List<PendingPhoto> = emptyList(),
    val open: String = DEFAULT_OPEN,
    val close: String = DEFAULT_CLOSE,
    val closedDays: Set<Int> = emptySet(),
    val perDay: Boolean = false,
    val days: List<DayInput> = defaultDays(),
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val instagram: String = "",
    val beanIds: Set<String> = emptySet(),
    val equipmentIds: Set<String> = emptySet(),
    val roasterIds: Set<String> = emptySet(),
    val brewMethodIds: Set<String> = emptySet(),
    val step: AddShopStep = AddShopStep.Basic,
    val resolvingAddress: Boolean = false,
    val locationHint: LocationHint? = null,
    val mapPickerOpen: Boolean = false,
    val submit: SubmitState = SubmitState.Idle,
) {
    val selectedCity: CatalogOption? get() = catalogs.cities.firstOrNull { it.id == cityId }
    val contacts: Contacts get() = Contacts(phone, email, website, instagram)
    val basicsIssues: List<ShopBasicsIssue> get() = validateShopBasics(name, address, cityId)
    val contactIssues: Set<ContactIssue> get() = validateContacts(contacts)
    val basicsValid: Boolean get() = basicsIssues.isEmpty()
    val contactsValid: Boolean get() = contactIssues.isEmpty()
    val busy: Boolean get() = submit == SubmitState.Sending || submit == SubmitState.Done
    val gpsPoint: Pair<Double, Double>? get() = if (latitude != null && longitude != null) latitude to longitude else null
}

internal fun defaultDays(open: String = DEFAULT_OPEN, close: String = DEFAULT_CLOSE, closed: Set<Int> = emptySet()) =
    WeekOrder.map { DayInput(it, closed = it in closed, open = open, close = close) }

internal sealed interface AddShopAction {
    data class Name(val value: String) : AddShopAction
    data class Address(val value: String) : AddShopAction
    data class Description(val value: String) : AddShopAction
    data class City(val id: String) : AddShopAction
    data class Price(val value: PriceRange?) : AddShopAction
    data class Phone(val value: String) : AddShopAction
    data class Email(val value: String) : AddShopAction
    data class Website(val value: String) : AddShopAction
    data class Instagram(val value: String) : AddShopAction

    data class AddPhotos(val photos: List<PendingPhoto>) : AddShopAction
    data class RemovePhoto(val index: Int) : AddShopAction
    data class AddMenuPhotos(val photos: List<PendingPhoto>) : AddShopAction
    data class RemoveMenuPhoto(val index: Int) : AddShopAction

    data class ToggleBean(val id: String) : AddShopAction
    data class ToggleEquipment(val id: String) : AddShopAction
    data class ToggleRoaster(val id: String) : AddShopAction
    data class ToggleBrewMethod(val id: String) : AddShopAction

    data class PerDay(val enabled: Boolean) : AddShopAction
    data class ToggleClosedDay(val day: Int) : AddShopAction
    data class Preset(val open: String, val close: String) : AddShopAction
    data class AdjustOpen(val minutes: Int) : AddShopAction
    data class AdjustClose(val minutes: Int) : AddShopAction
    data class SetDayOpen(val day: Int, val open: Boolean) : AddShopAction
    data class AdjustDayOpen(val day: Int, val minutes: Int) : AddShopAction
    data class AdjustDayClose(val day: Int, val minutes: Int) : AddShopAction

    data object OpenMap : AddShopAction
    data object DismissMap : AddShopAction
    data class MapPicked(val latitude: Double, val longitude: Double, val address: String) : AddShopAction
    data object LocationGranted : AddShopAction
    data object LocationDenied : AddShopAction

    data object RetryCatalogs : AddShopAction
    data object Next : AddShopAction
    data object Previous : AddShopAction
    data object DismissFailure : AddShopAction
}

internal sealed interface AddShopEvent
