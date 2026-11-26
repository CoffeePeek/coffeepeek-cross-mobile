package com.coffeepeek.admin.di.ownership

import com.coffeepeek.feature.ownership.impl.*

data class IosOwnershipState(val signedIn: Boolean, val generation: Int, val destination: String, val loading: Boolean,
    val busy: Boolean, val problem: String?, val saved: Boolean, val dirty: Boolean, val reloadRequired: Boolean,
    val confirmPublish: Boolean, val confirmDiscard: Boolean, val reloadAfterDiscard: Boolean,
    val claims: List<IosOwnershipClaim>, val claimsPage: Int, val claimsNext: Boolean,
    val businesses: List<IosOwnershipBusiness>, val roastersPage: Int, val roastersNext: Boolean,
    val contact: String, val evidence: String, val evidencePhotos: List<String>, val claim: IosOwnershipClaim?,
    val editorKind: String?, val editorPhotos: List<String>, val fields: List<IosOwnershipField>)
data class IosOwnershipBusiness(val id: String, val kind: String, val title: String, val address: String?, val cover: String?)
data class IosOwnershipField(val name: String, val value: String)
data class IosOwnershipClaim(val id: String, val title: String, val slug: String, val status: String, val access: Boolean,
    val contact: String, val evidence: String, val created: String?, val reviewed: String?, val decision: String?, val photos: List<String>)

internal fun OwnershipNativeSnapshot.iosState() = IosOwnershipState(signedIn, generation, destination, loading,
    busy, problem, saved, dirty, reloadRequired, confirmPublish, confirmDiscard, reloadAfterDiscard,
    claims.map { it.iosClaim() }, claimsPage, claimsNext,
    businesses.map { IosOwnershipBusiness(it.id, it.kind, it.title, it.address, it.cover) }, roastersPage, roastersNext,
    contact, evidence, evidencePhotos, claim?.iosClaim(), editorKind, editorPhotos, fields.map { IosOwnershipField(it.name, it.value) })

private fun OwnershipNativeClaim.iosClaim() = IosOwnershipClaim(id, title, slug, status, access,
    contact, evidence, created, reviewed, decision, photos)
