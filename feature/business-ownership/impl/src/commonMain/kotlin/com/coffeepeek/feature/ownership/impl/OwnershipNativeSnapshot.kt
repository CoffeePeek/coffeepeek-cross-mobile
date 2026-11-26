package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.ui.compose.formatOwnershipClaimDate
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*

class OwnershipNativeSnapshot internal constructor(state: OwnershipState, tab: Boolean) {
    val signedIn = state.account != null && state.problem != OwnershipProblem.SignedOut
    val generation = state.generation
    val destination = state.destination.name
    val loading = state.loading
    val busy = state.busy
    val problem = state.problem?.name
    val saved = state.saved
    val dirty = state.dirty
    val reloadRequired = state.reloadRequired
    val confirmPublish = state.confirmPublish
    val confirmDiscard = state.confirmDiscard
    val reloadAfterDiscard = state.reloadAfterDiscard
    val claims = state.claims?.items.orEmpty().map(::OwnershipNativeClaim)
    val claimsPage = state.claimsPage
    val claimsNext = state.claims?.hasNext == true
    val businesses = (state.shops + state.roasters?.items.orEmpty()).map { business ->
        val profile = state.cardProfiles[business.kind to business.id]
        OwnershipNativeBusiness(business.id, business.kind.name, business.name, profile?.address,
            "card:${business.kind.name}:${business.id}:${profile?.revision}".takeIf { tab && profile?.photos?.isNotEmpty() == true })
    }
    val roastersPage = state.roastersPage
    val roastersNext = state.roasters?.hasNext == true
    val contact = state.submission.contact
    val evidence = state.submission.evidence
    val evidencePhotos = state.submission.photos.map { "evidence:${it.key}" }
    val claim = state.selectedClaim?.let(::OwnershipNativeClaim)
    val editorKind = state.profile?.kind?.name
    val editorPhotos = state.draft?.photos.orEmpty().map { "draft:${it.key}" }
    val fields = state.draft?.let { draft -> listOf(
        OwnershipNativeField("Name", draft.name), OwnershipNativeField("About", draft.about),
        OwnershipNativeField("Phone", draft.phone), OwnershipNativeField("Email", draft.email),
        OwnershipNativeField("Site", draft.site), OwnershipNativeField("Instagram", draft.instagram),
        OwnershipNativeField("Address", draft.address), OwnershipNativeField("Latitude", draft.latitude), OwnershipNativeField("Longitude", draft.longitude),
    ).filter { state.profile?.kind == BusinessKind.CoffeeShop || it.name !in setOf("Phone", "Email") } }.orEmpty()
}

class OwnershipNativeBusiness internal constructor(val id: String, val kind: String, val title: String,
    val address: String?, val cover: String?)
class OwnershipNativeField internal constructor(val name: String, val value: String)

class OwnershipNativeClaim internal constructor(claim: OwnershipClaim) {
    val id = claim.id
    val title = claim.name
    val slug = claim.slug
    val status = claim.status.name
    val access = claim.hasCurrentAccess
    val contact = claim.contact
    val evidence = claim.evidence
    val created = formatOwnershipClaimDate(claim.createdAt)
    val reviewed = claim.reviewedAt?.let { formatOwnershipClaimDate(it) }
    val decision = claim.decisionReason
    val photos = claim.evidencePhotos.map { "claim:${it.key}" }
}
