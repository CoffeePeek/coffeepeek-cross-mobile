package com.coffeepeek.feature.ownership.impl.ui.compose.model

import com.coffeepeek.feature.ownership.domain.model.*

internal enum class OwnershipDestination { Overview, Submit, Claim, Editor }
internal enum class ClaimField { Slug, Contact, Evidence }
internal data class OwnershipState(
    val account: String? = null, val generation: Int = 0,
    val destination: OwnershipDestination = OwnershipDestination.Overview,
    val loading: Boolean = false, val busy: Boolean = false, val problem: OwnershipProblem? = null,
    val claims: BusinessPage<OwnershipClaim>? = null, val shops: List<OwnedBusiness> = emptyList(),
    val roasters: BusinessPage<OwnedBusiness>? = null, val claimsPage: Int = 1, val roastersPage: Int = 1,
    val cardProfiles: Map<Pair<BusinessKind, String>, BusinessProfile> = emptyMap(),
    val submission: ClaimSubmission = ClaimSubmission(BusinessKind.CoffeeShop, "", "", ""),
    val selectedClaim: OwnershipClaim? = null, val selectedClaimId: String? = null,
    val editingBusiness: OwnedBusiness? = null, val reloadAfterDiscard: Boolean = false, val profile: BusinessProfile? = null,
    val draft: ProfileDraft? = null, val reloadRequired: Boolean = false,
    val confirmPublish: Boolean = false, val confirmDiscard: Boolean = false,
    val saved: Boolean = false,
) {
    val dirty: Boolean get() = profile != null && draft != null && draft != ProfileDraft.from(profile)
}
internal sealed interface OwnershipAction {
    data object Refresh : OwnershipAction
    data object Back : OwnershipAction
    data object Login : OwnershipAction
    data object NewClaim : OwnershipAction
    data object Submit : OwnershipAction
    data object AskSave : OwnershipAction
    data object Save : OwnershipAction
    data object CancelConfirmation : OwnershipAction
    data object Discard : OwnershipAction
    data object AskReload : OwnershipAction
    data object Reload : OwnershipAction
    data class Kind(val kind: BusinessKind) : OwnershipAction
    data class ClaimText(val field: ClaimField, val value: String) : OwnershipAction
    data class OpenClaim(val id: String) : OwnershipAction
    data class OpenBusiness(val kind: BusinessKind, val id: String) : OwnershipAction
    data class ProfileText(val field: ProfileField, val value: String) : OwnershipAction
    data class AddEvidence(val generation: Int, val photos: List<BusinessPhoto>) : OwnershipAction
    data class MoveEvidence(val from: Int, val to: Int) : OwnershipAction
    data class RemoveEvidence(val index: Int) : OwnershipAction
    data class AddPhotos(val generation: Int, val photos: List<BusinessPhoto>) : OwnershipAction
    data class RemovePhoto(val index: Int) : OwnershipAction
    data class MovePhoto(val from: Int, val to: Int) : OwnershipAction
    data class ClaimsPage(val page: Int) : OwnershipAction
    data class RoastersPage(val page: Int) : OwnershipAction
}
internal enum class OwnershipEvent { Close, Login }
