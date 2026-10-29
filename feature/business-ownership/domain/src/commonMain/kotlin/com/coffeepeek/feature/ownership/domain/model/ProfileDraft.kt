package com.coffeepeek.feature.ownership.domain.model

enum class ProfileField { Name, About, Phone, Email, Site, Instagram, Address, Latitude, Longitude }
data class ProfileDraft(
    val name: String = "", val about: String = "", val phone: String = "", val email: String = "",
    val site: String = "", val instagram: String = "", val address: String = "",
    val latitude: String = "", val longitude: String = "", val photos: List<BusinessPhoto> = emptyList(),
) {
    fun edit(field: ProfileField, value: String): ProfileDraft = when (field) {
        ProfileField.Name -> copy(name = value)
        ProfileField.About -> copy(about = value)
        ProfileField.Phone -> copy(phone = value)
        ProfileField.Email -> copy(email = value)
        ProfileField.Site -> copy(site = value)
        ProfileField.Instagram -> copy(instagram = value)
        ProfileField.Address -> copy(address = value)
        ProfileField.Latitude -> copy(latitude = value)
        ProfileField.Longitude -> copy(longitude = value)
    }
    fun applyTo(original: BusinessProfile): BusinessProfile {
        validate()
        return original.copy(name = name.trim(), about = about.trim(), phone = phone.trim(), email = email.trim(),
            site = site.trim(), instagram = instagram.trim(), address = address.trim(),
            latitude = latitude.trim().toDoubleOrNull(), longitude = longitude.trim().toDoubleOrNull(), photos = photos)
    }
    fun validate() {
        if (name.isBlank() || name.trim().length > 200 || about.length > 4000 || photos.size > 10 ||
            !coordinate(latitude, -90.0, 90.0) || !coordinate(longitude, -180.0, 180.0) ||
            latitude.isBlank() != longitude.isBlank() || !link(site.trim()) || !link(instagram.trim())) {
            throw OwnershipFailure(OwnershipProblem.Validation)
        }
    }
    companion object {
        fun from(profile: BusinessProfile) = ProfileDraft(profile.name, profile.about, profile.phone,
            profile.email, profile.site, profile.instagram, profile.address,
            profile.latitude?.toString().orEmpty(), profile.longitude?.toString().orEmpty(), profile.photos)
    }
}
private fun coordinate(value: String, minimum: Double, maximum: Double): Boolean =
    value.isBlank() || value.trim().toDoubleOrNull()?.let { it.isFinite() && it in minimum..maximum } == true
private fun link(value: String): Boolean = value.isBlank() ||
    ((value.startsWith("https://") || value.startsWith("http://")) && value.substringAfter("://").isNotBlank() && value.none(Char::isWhitespace))
