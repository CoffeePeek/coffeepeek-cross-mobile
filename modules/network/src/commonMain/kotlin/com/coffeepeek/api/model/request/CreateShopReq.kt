package com.coffeepeek.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateShopReq(
    @SerialName("name")          val name: String,
    @SerialName("address")       val address: String,
    @SerialName("city")        val cityId: String,
    @SerialName("latitude")      val latitude: Double?       = null,
    @SerialName("longitude")     val longitude: Double?      = null,
    @SerialName("description")   val description: String?      = null,
    @SerialName("priceRange")    val priceRange: String?       = null,
    @SerialName("shopContact")   val shopContact: CreateShopContactReq? = null,
    @SerialName("schedules")     val schedules: List<ScheduleReq>? = null,
    @SerialName("shopPhotos")    val shopPhotos: List<UploadedPhotoReq>? = null,
    @SerialName("menuPhotos")    val menuPhotos: List<UploadedPhotoReq>? = null,
    @SerialName("equipments")  val equipmentIds: List<String>? = null,
    @SerialName("beans") val coffeeBeanIds: List<String>? = null,
    @SerialName("roasters")    val roasterIds: List<String>? = null,
    @SerialName("brewMethods") val brewMethodIds: List<String>? = null,
)

@Serializable
data class CreateShopContactReq(
    @SerialName("phoneNumber")   val phoneNumber: String?   = null,
    @SerialName("email")         val email: String?         = null,
    @SerialName("siteLink")      val siteLink: String?      = null,
    @SerialName("instagramLink") val instagramLink: String? = null,
)
