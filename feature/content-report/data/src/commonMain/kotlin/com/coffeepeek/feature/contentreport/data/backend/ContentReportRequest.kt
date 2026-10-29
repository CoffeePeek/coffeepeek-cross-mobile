@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.coffeepeek.feature.contentreport.data.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable internal data class ContentReportRequest(val text: String)
@Serializable internal data class ContentReportResponse(
    @JsonNames("IsSuccess") val isSuccess: Boolean,
    @JsonNames("Message") val message: String? = null,
    @JsonNames("Data") val data: JsonElement? = null,
)
