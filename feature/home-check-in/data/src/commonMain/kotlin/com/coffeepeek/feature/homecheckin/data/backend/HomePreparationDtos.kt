@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.homecheckin.data.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable internal data class HomeEnvelope<T>(val isSuccess: Boolean, val message: String? = null, val data: T? = null)
@Serializable internal data class HomePageDto(val items: List<HomeDto> = emptyList(), val totalCount: Int = 0)
@Serializable internal data class HomeCalendarDto(val preparedOn: String, val brewCount: Int)
@Serializable internal data class HomePhotoDto(
    val id: String = "", @JsonNames("fullUrl") val url: String = "",
    val sortIndex: Int = 0, val variants: List<HomePhotoVariantDto>? = null,
)
@Serializable internal data class HomePhotoVariantDto(
    val purpose: String = "", val contentType: String = "", val url: String = "",
    val width: Int = 0, val height: Int = 0, val sizeBytes: Long = 0,
)
@Serializable internal data class HomeDto(
    val id: String,
    val preparedAtUtc: String,
    val preparedOn: String,
    val createdAtUtc: String = "",
    val text: String? = null,
    val brewMethodCode: String? = null,
    val coffeeProductId: String? = null,
    val beanName: String? = null,
    val doseGrams: Double? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
    val drink: JsonElement? = null,
    val photos: List<HomePhotoDto> = emptyList(),
    val drinkPhoto: JsonElement? = null,
    val visibility: String? = null,
    val moderationState: String? = null,
)
@Serializable internal data class HomeEditDto(
    val preparedAt: String,
    val preparedOn: String?,
    val brewMethodCode: String?,
    val coffeeProductSlug: String?,
    val coffeeProductId: String?,
    val customBeanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val text: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val visibility: String? = null,
)
@Serializable internal data class HomeCreateDto(
    val preparedAt: String,
    val preparedOn: String?,
    val brewMethodCode: String?,
    val coffeeProductSlug: String?,
    val coffeeProductId: String?,
    val customBeanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val text: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<UploadedPhotoDto>,
    val drinkPhoto: UploadedPhotoDto?,
    val visibility: String,
)
@Serializable internal data class PhotoUploadRequestDto(val sizeBytes: Int, val fileName: String, val contentType: String)
@Serializable internal data class PhotoUploadUrlDto(val uploadUrl: String, val storageKey: String)
@Serializable internal data class UploadedPhotoDto(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
