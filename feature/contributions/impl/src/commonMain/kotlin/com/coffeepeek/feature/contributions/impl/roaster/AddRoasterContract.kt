package com.coffeepeek.feature.contributions.impl.roaster

import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmitted
import com.coffeepeek.feature.contributions.domain.rules.RoasterIssue
import com.coffeepeek.feature.contributions.domain.rules.validateRoaster

internal sealed interface RoasterSubmitState {
    data object Idle : RoasterSubmitState
    data object Sending : RoasterSubmitState
    data class Done(val result: RoasterSubmitted) : RoasterSubmitState

    /** [serverMessage] is the server's own wording; null shows the default text. */
    data class Failed(val serverMessage: String?) : RoasterSubmitState
}

internal data class AddRoasterState(
    /** Null until the first answer about the account; a guest cannot send a roaster. */
    val signedIn: Boolean? = null,
    val cities: List<CatalogOption> = emptyList(),
    val citiesLoading: Boolean = true,
    val citiesFailed: Boolean = false,
    val name: String = "",
    val about: String = "",
    val cityId: String? = null,
    val address: String = "",
    val instagram: String = "",
    val website: String = "",
    val photos: List<PendingPhoto> = emptyList(),
    val submit: RoasterSubmitState = RoasterSubmitState.Idle,
) {
    val issues: Set<RoasterIssue> get() = validateRoaster(name, cityId, address, instagram, website)
    val valid: Boolean get() = issues.isEmpty()
    val busy: Boolean get() = submit == RoasterSubmitState.Sending || submit is RoasterSubmitState.Done
}

internal sealed interface AddRoasterAction {
    data class Name(val value: String) : AddRoasterAction
    data class About(val value: String) : AddRoasterAction
    data class City(val id: String) : AddRoasterAction
    data class Address(val value: String) : AddRoasterAction
    data class Instagram(val value: String) : AddRoasterAction
    data class Website(val value: String) : AddRoasterAction
    data class AddPhotos(val photos: List<PendingPhoto>) : AddRoasterAction
    data class RemovePhoto(val index: Int) : AddRoasterAction
    data object RetryCities : AddRoasterAction
    data object Submit : AddRoasterAction
    data object DismissFailure : AddRoasterAction
}

internal sealed interface AddRoasterEvent
