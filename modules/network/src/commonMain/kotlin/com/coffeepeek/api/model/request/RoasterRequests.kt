package com.coffeepeek.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoasterSubmissionReq(
    @SerialName("name") val name: String,
    @SerialName("about") val about: String? = null,
    @SerialName("city") val cityId: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("instagramLink") val instagramLink: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
    @SerialName("photos") val photos: List<UploadedPhotoReq>? = null,
)
