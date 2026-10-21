package com.coffeepeek.feature.contributions.impl.shop

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.NewShop
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.DayInput
import com.coffeepeek.feature.contributions.domain.rules.perDayWeek
import com.coffeepeek.feature.contributions.domain.rules.shiftClock
import com.coffeepeek.feature.contributions.domain.rules.unifiedWeek
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The five-step shop form as the legacy wizard kept it. The rule over the legacy: one account at a time. A guest cannot fill the form, and a change of
 * account starts an empty form, so nothing typed for one account is ever sent for another; late answers of a previous account are dropped.
 */
internal class AddShopViewModel(
    private val repository: ContributionsRepository,
    private val catalogs: ContributionCatalogs,
    private val location: ContributionLocation,
    private val account: ContributionsAccount,
) : MviViewModel<AddShopState, AddShopAction, AddShopEvent>(AddShopState()) {
    private var key: String? = null
    private var generation = 0L
    private var work: Job? = null
    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { accountKey ->
                key = accountKey
                generation++
                work?.cancel(); locationJob?.cancel()
                if (accountKey == null) updateState { AddShopState(signedIn = false, catalogsLoading = false) }
                else { updateState { AddShopState(signedIn = true) }; loadCatalogs() }
            }
        }
    }

    override suspend fun handleActionInternal(action: AddShopAction) {
        if (currentState.signedIn != true) return
        when (action) {
            is AddShopAction.Name -> updateState { copy(name = action.value.take(ContributionLimits.SHOP_NAME)) }
            is AddShopAction.Address -> updateState { copy(address = action.value, latitude = null, longitude = null, locationHint = null) }
            is AddShopAction.Description -> updateState { copy(description = action.value) }
            is AddShopAction.City -> updateState { copy(cityId = action.id) }
            is AddShopAction.Price -> updateState { copy(priceRange = action.value) }
            is AddShopAction.Phone -> updateState { copy(phone = action.value) }
            is AddShopAction.Email -> updateState { copy(email = action.value) }
            is AddShopAction.Website -> updateState { copy(website = action.value) }
            is AddShopAction.Instagram -> updateState { copy(instagram = action.value) }
            is AddShopAction.AddPhotos -> updateState { copy(photos = photos + action.photos.take((ContributionLimits.SHOP_PHOTOS - photos.size).coerceAtLeast(0))) }
            is AddShopAction.RemovePhoto -> updateState { copy(photos = photos.filterIndexed { i, _ -> i != action.index }) }
            is AddShopAction.AddMenuPhotos -> updateState { copy(menuPhotos = menuPhotos + action.photos.take((ContributionLimits.MENU_PHOTOS - menuPhotos.size).coerceAtLeast(0))) }
            is AddShopAction.RemoveMenuPhoto -> updateState { copy(menuPhotos = menuPhotos.filterIndexed { i, _ -> i != action.index }) }
            is AddShopAction.ToggleBean -> updateState { copy(beanIds = beanIds.toggle(action.id)) }
            is AddShopAction.ToggleEquipment -> updateState { copy(equipmentIds = equipmentIds.toggle(action.id)) }
            is AddShopAction.ToggleRoaster -> updateState { copy(roasterIds = roasterIds.toggle(action.id)) }
            is AddShopAction.ToggleBrewMethod -> updateState { copy(brewMethodIds = brewMethodIds.toggle(action.id)) }
            is AddShopAction.PerDay -> setPerDay(action.enabled)
            is AddShopAction.ToggleClosedDay -> updateState { copy(closedDays = closedDays.toggleDay(action.day)) }
            is AddShopAction.Preset -> updateState { copy(open = action.open, close = action.close) }
            is AddShopAction.AdjustOpen -> updateState { copy(open = shiftClock(open, action.minutes)) }
            is AddShopAction.AdjustClose -> updateState { copy(close = shiftClock(close, action.minutes)) }
            is AddShopAction.SetDayOpen -> updateDay(action.day) { copy(closed = !action.open) }
            is AddShopAction.AdjustDayOpen -> updateDay(action.day) { copy(open = shiftClock(open, action.minutes)) }
            is AddShopAction.AdjustDayClose -> updateDay(action.day) { copy(close = shiftClock(close, action.minutes)) }
            AddShopAction.OpenMap -> updateState { copy(mapPickerOpen = true, locationHint = null) }
            AddShopAction.DismissMap -> updateState { copy(mapPickerOpen = false) }
            is AddShopAction.MapPicked -> updateState {
                copy(address = action.address, latitude = action.latitude, longitude = action.longitude, mapPickerOpen = false, locationHint = null,
                    cityId = cityId ?: matchCity(action.address, catalogs.cities))
            }
            AddShopAction.LocationGranted -> fillFromLocation()
            AddShopAction.LocationDenied -> updateState { copy(resolvingAddress = false, locationHint = LocationHint.PermissionDenied) }
            AddShopAction.RetryCatalogs -> loadCatalogs()
            AddShopAction.Next -> next()
            AddShopAction.Previous -> previous()
            AddShopAction.DismissFailure -> updateState { copy(submit = SubmitState.Idle) }
        }
    }

    private fun loadCatalogs() {
        val mine = generation
        updateState { copy(catalogsLoading = true, catalogsFailed = false) }
        viewModelScope.launch {
            val result = catalogs.catalogs()
            if (mine != generation) return@launch
            result.onSuccess { loaded ->
                updateState {
                    copy(catalogs = loaded, catalogsLoading = false, catalogsFailed = false,
                        cityId = cityId ?: loaded.cities.singleOrNull()?.id ?: matchCity(address, loaded.cities))
                }
            }.onFailure { updateState { copy(catalogsLoading = false, catalogsFailed = true) } }
        }
    }

    private fun fillFromLocation() {
        val mine = generation
        locationJob?.cancel()
        updateState { copy(resolvingAddress = true, locationHint = null) }
        locationJob = viewModelScope.launch {
            val point = location.lastKnown()
            if (mine != generation) return@launch
            if (point == null) {
                updateState { copy(resolvingAddress = false, locationHint = LocationHint.NotFound) }
                return@launch
            }
            val address = location.addressOf(point)?.takeIf(String::isNotBlank)
            if (mine != generation) return@launch
            updateState { applyLocation(point, address) }
        }
    }

    private fun AddShopState.applyLocation(point: GeoPoint, address: String?): AddShopState = when {
        address == null -> copy(latitude = point.latitude, longitude = point.longitude, resolvingAddress = false, locationHint = LocationHint.NoAddress)
        // The address the user already typed is not overwritten by the automatic fill.
        this.address.isNotBlank() -> copy(latitude = latitude ?: point.latitude, longitude = longitude ?: point.longitude, resolvingAddress = false)
        else -> copy(address = address, latitude = point.latitude, longitude = point.longitude, resolvingAddress = false, locationHint = null,
            cityId = cityId ?: matchCity(address, catalogs.cities))
    }

    private fun setPerDay(enabled: Boolean) = updateState {
        if (!enabled) copy(perDay = false) else copy(perDay = true, days = defaultDays(open, close, closedDays))
    }

    private fun updateDay(day: Int, transform: DayInput.() -> DayInput) =
        updateState { copy(days = days.map { if (it.day == day) it.transform() else it }) }

    private fun next() {
        val state = currentState
        if (state.busy) return
        when (state.step) {
            AddShopStep.Basic -> if (state.basicsValid) updateState { copy(step = AddShopStep.Contacts) }
            AddShopStep.Contacts -> if (state.contactsValid) updateState { copy(step = AddShopStep.Photos) }
            AddShopStep.Photos -> updateState { copy(step = AddShopStep.Features) }
            AddShopStep.Features -> updateState { copy(step = AddShopStep.Schedule) }
            AddShopStep.Schedule -> submit()
        }
    }

    private fun previous() = updateState {
        if (busy) this else copy(step = when (step) {
            AddShopStep.Contacts -> AddShopStep.Basic
            AddShopStep.Photos -> AddShopStep.Contacts
            AddShopStep.Features -> AddShopStep.Photos
            AddShopStep.Schedule -> AddShopStep.Features
            AddShopStep.Basic -> step
        })
    }

    private fun submit() {
        val state = currentState
        val city = state.cityId ?: return
        if (state.catalogsLoading || !state.basicsValid || !state.contactsValid || state.busy) return
        updateState { copy(submit = SubmitState.Sending) }
        val mine = generation
        work = viewModelScope.launch {
            val result = repository.submitShop(state.toNewShop(city))
            if (mine != generation) return@launch
            result.onSuccess { updateState { copy(submit = SubmitState.Done) } }
                .onFailure { failure -> updateState { copy(submit = SubmitState.Failed((failure as? ContributionFailure)?.serverMessage)) } }
        }
    }
}

private fun AddShopState.toNewShop(city: String) = NewShop(
    name = name.trim(),
    address = address.trim(),
    cityId = city,
    latitude = latitude,
    longitude = longitude,
    description = description.trim().takeIf(String::isNotEmpty),
    priceRange = priceRange,
    contacts = contacts,
    equipmentIds = equipmentIds.toList(),
    beanIds = beanIds.toList(),
    roasterIds = roasterIds.toList(),
    brewMethodIds = brewMethodIds.toList(),
    photos = photos,
    menuPhotos = menuPhotos,
    week = if (perDay) perDayWeek(days) else unifiedWeek(open, close, closedDays),
)

private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
private fun Set<Int>.toggleDay(day: Int) = if (day in this) this - day else this + day

/** A city whose name appears in the address, e.g. after the map or the location filled it. */
private fun matchCity(address: String, cities: List<CatalogOption>): String? =
    cities.firstOrNull { address.lowercase().contains(it.name.lowercase()) }?.id
