package com.coffeepeek.feature.contributions.impl.roaster

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.NewRoaster
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.instagramLinkOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The roaster form. The rule over the legacy: one account at a time. A guest cannot fill the form, and a change of account starts an empty form, so
 * nothing typed for one account is ever sent for another; late answers of a previous account are dropped. The city is only needed with an address.
 */
internal class AddRoasterViewModel(
    private val repository: ContributionsRepository,
    private val catalogs: ContributionCatalogs,
    private val account: ContributionsAccount,
) : MviViewModel<AddRoasterState, AddRoasterAction, AddRoasterEvent>(AddRoasterState()) {
    private var generation = 0L
    private var work: Job? = null

    init {
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { accountKey ->
                generation++
                work?.cancel()
                if (accountKey == null) updateState { AddRoasterState(signedIn = false, citiesLoading = false) }
                else { updateState { AddRoasterState(signedIn = true) }; loadCities() }
            }
        }
    }

    override suspend fun handleActionInternal(action: AddRoasterAction) {
        if (currentState.signedIn != true || currentState.busy && action != AddRoasterAction.DismissFailure) return
        when (action) {
            is AddRoasterAction.Name -> updateState { copy(name = action.value.take(ContributionLimits.ROASTER_NAME)) }
            is AddRoasterAction.About -> updateState { copy(about = action.value) }
            is AddRoasterAction.City -> updateState { copy(cityId = action.id) }
            is AddRoasterAction.Address -> updateState { copy(address = action.value, cityId = cityId ?: cities.singleOrNull()?.id.takeIf { action.value.isNotBlank() }) }
            is AddRoasterAction.Instagram -> updateState { copy(instagram = action.value) }
            is AddRoasterAction.Website -> updateState { copy(website = action.value) }
            is AddRoasterAction.AddPhotos -> updateState { copy(photos = photos + action.photos.take((ContributionLimits.ROASTER_PHOTOS - photos.size).coerceAtLeast(0))) }
            is AddRoasterAction.RemovePhoto -> updateState { copy(photos = photos.filterIndexed { i, _ -> i != action.index }) }
            AddRoasterAction.RetryCities -> loadCities()
            AddRoasterAction.Submit -> submit()
            AddRoasterAction.DismissFailure -> updateState { copy(submit = RoasterSubmitState.Idle) }
        }
    }

    private fun loadCities() {
        val mine = generation
        updateState { copy(citiesLoading = true, citiesFailed = false) }
        viewModelScope.launch {
            val result = catalogs.catalogs()
            if (mine != generation) return@launch
            result.onSuccess { loaded -> updateState { copy(cities = loaded.cities, citiesLoading = false, citiesFailed = false) } }
                .onFailure { updateState { copy(citiesLoading = false, citiesFailed = true) } }
        }
    }

    private fun submit() {
        val state = currentState
        if (!state.valid || state.busy) return
        updateState { copy(submit = RoasterSubmitState.Sending) }
        val mine = generation
        work = viewModelScope.launch {
            val result = repository.submitRoaster(state.toNewRoaster())
            if (mine != generation) return@launch
            result.onSuccess { sent -> updateState { copy(submit = RoasterSubmitState.Done(sent)) } }
                .onFailure { failure -> updateState { copy(submit = RoasterSubmitState.Failed((failure as? ContributionFailure)?.serverMessage)) } }
        }
    }
}

private fun AddRoasterState.toNewRoaster(): NewRoaster {
    val address = address.trim().takeIf(String::isNotEmpty)
    return NewRoaster(
        name = name.trim(),
        about = about.trim().takeIf(String::isNotEmpty),
        cityId = cityId.takeIf { address != null },
        address = address,
        instagramLink = instagramLinkOf(instagram),
        siteLink = website.trim().takeIf(String::isNotEmpty),
        photos = photos,
    )
}
