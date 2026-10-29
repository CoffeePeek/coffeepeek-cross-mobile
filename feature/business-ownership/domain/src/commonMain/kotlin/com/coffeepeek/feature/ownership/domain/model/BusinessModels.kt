package com.coffeepeek.feature.ownership.domain.model

import kotlinx.coroutines.flow.Flow

enum class BusinessKind { CoffeeShop, Roaster }
enum class ClaimStatus { Pending, Approved, Rejected }
enum class OwnershipProblem { Unavailable, SignedOut, NotFound, Conflict, Validation, PartialSave }
class OwnershipFailure(val problem: OwnershipProblem) : Exception(problem.name)

data class BusinessPage<T>(val items: List<T>, val totalCount: Int, val page: Int, val pageSize: Int) {
    val hasNext: Boolean get() = page * pageSize < totalCount
}
data class OwnershipClaim(
    val id: String, val kind: BusinessKind, val businessId: String, val name: String,
    val slug: String, val contact: String, val evidence: String, val status: ClaimStatus,
    val createdAt: String, val decisionReason: String?, val reviewedAt: String?, val hasCurrentAccess: Boolean,
)
data class ClaimSubmission(val kind: BusinessKind, val slug: String, val contact: String, val evidence: String) {
    fun validated(): ClaimSubmission {
        val value = copy(slug = publicSlug(kind, slug), contact = contact.trim(), evidence = evidence.trim())
        if (value.slug.isEmpty() || value.slug.length > 200 || '/' in value.slug ||
            value.contact.isEmpty() || value.contact.length > 320 ||
            value.evidence.isEmpty() || value.evidence.length > 4000) throw OwnershipFailure(OwnershipProblem.Validation)
        return value
    }
}
data class OwnedBusiness(val id: String, val kind: BusinessKind, val name: String)
data class BusinessPhoto(
    val key: String, val id: String? = null, val storageKey: String? = null, val fileName: String,
    val contentType: String = "image/jpeg", val url: String? = null, val size: Long = 0,
    val bytes: ByteArray? = null,
)
data class BusinessProfile(
    val id: String, val kind: BusinessKind, val revision: Long, val name: String,
    val about: String = "", val phone: String = "", val email: String = "",
    val site: String = "", val instagram: String = "", val cityId: String? = null,
    val address: String = "", val latitude: Double? = null, val longitude: Double? = null,
    val photos: List<BusinessPhoto> = emptyList(),
)
interface OwnershipAccount {
    fun current(): String?
    fun observe(): Flow<String?>
}
interface BusinessPhotoUploader {
    suspend fun upload(kind: BusinessKind, photos: List<BusinessPhoto>): Result<List<BusinessPhoto>>
}


private fun publicSlug(kind: BusinessKind, raw: String): String {
    val value = raw.trim()
    if (!value.contains('/')) return value
    val prefix = if (kind == BusinessKind.CoffeeShop) "coffee-shops/" else "roasters/"
    val path = when {
        value.startsWith("https://coffeepeek.by/") -> value.removePrefix("https://coffeepeek.by/")
        value.startsWith("https://www.coffeepeek.by/") -> value.removePrefix("https://www.coffeepeek.by/")
        else -> throw OwnershipFailure(OwnershipProblem.Validation)
    }.substringBefore('?').substringBefore('#').trimEnd('/')
    if (!path.startsWith(prefix)) throw OwnershipFailure(OwnershipProblem.Validation)
    return path.removePrefix(prefix)
}
