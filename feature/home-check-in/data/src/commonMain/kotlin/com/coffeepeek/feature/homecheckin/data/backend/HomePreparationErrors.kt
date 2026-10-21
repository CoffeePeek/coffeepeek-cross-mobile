package com.coffeepeek.feature.homecheckin.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField.*
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal suspend fun <T> homeRequestResult(block: suspend () -> T): Result<T> {
    val result = requestResult(block)
    val failure = result.exceptionOrNull() ?: return result
    val safe = when (failure) {
        is HomePreparationFailure -> failure
        is ResponseException -> {
            val body = try { failure.response.bodyAsText() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { "" }
            homeServerFailure(failure.response.status.value, body)
        }
        is HttpRequestTimeoutException -> HomePreparationFailure(HomePreparationIssue.Timeout)
        is IOException -> HomePreparationFailure(HomePreparationIssue.Offline)
        is SerializationException -> HomePreparationFailure(HomePreparationIssue.InvalidResponse)
        // Existing gateway validator can reject 5xx before the feature reads the body.
        else -> HomePreparationFailure(if (failure.message == "Сервис временно недоступен. Попробуйте чуть позже.")
            HomePreparationIssue.Unavailable else HomePreparationIssue.RequestFailed)
    }
    return Result.failure(safe)
}

internal fun homeServerFailure(status: Int, body: String): HomePreparationFailure {
    val payload = try { Json.parseToJsonElement(body) as? JsonObject } catch (_: IllegalArgumentException) { null }
    val code = payload.string("errorCode")
    val general = when {
        status == 401 || code == "UNAUTHORIZED" -> HomePreparationIssue.Unauthorized
        status == 403 || code == "FORBIDDEN" -> HomePreparationIssue.Forbidden
        status == 429 -> HomePreparationIssue.RateLimited
        status >= 500 || code in setOf("INTERNAL_ERROR", "SERVICE_UNAVAILABLE") -> HomePreparationIssue.Unavailable
        else -> null
    }
    if (general != null) return HomePreparationFailure(general)
    val fields = linkedMapOf<HomePreparationField, HomePreparationIssue>()
    (payload?.value("errors") as? JsonObject)?.forEach { (key, value) ->
        val messages = when (value) {
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> listOfNotNull(value.contentOrNull)
            else -> emptyList()
        }
        if (messages.isNotEmpty()) {
            val known = messages.firstNotNullOfOrNull { knownFieldErrors[it] }
            val field = fieldForKey(key) ?: known?.first
            if (field != null) fields[field] = known?.takeIf { it.first == field }?.second ?: field.defaultError()
        }
    }
    knownFieldErrors[payload.string("message")]?.let { (field, issue) -> fields.getOrPut(field) { issue } }
    val issue = when {
        fields.isNotEmpty() -> HomePreparationIssue.InvalidInput
        status == 404 || code == "NOT_FOUND" -> HomePreparationIssue.NotFound
        status == 409 || code == "CONFLICT" -> HomePreparationIssue.Conflict
        status == 400 || status == 422 || code == "VALIDATION_FAILED" -> HomePreparationIssue.InvalidInput
        else -> HomePreparationIssue.RequestFailed
    }
    return HomePreparationFailure(issue, fields)
}

private fun JsonObject?.value(key: String) = this?.entries?.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
private fun JsonObject?.string(key: String) = (value(key) as? JsonPrimitive)?.contentOrNull

private fun fieldForKey(key: String): HomePreparationField? {
    val parts = key.lowercase().split('.', '[', ']')
    return when {
        parts.any { it == "photos" || it == "drinkphoto" } -> Photos
        parts.any { it == "preparedat" || it == "preparedon" } -> Date
        "brewmethodcode" in parts -> Method
        parts.any { it in setOf("coffeeproductslug", "coffeeproductid", "custombeanname") } -> Bean
        "dosegrams" in parts -> Dose
        "brewdurationseconds" in parts -> Duration
        "tastescore" in parts -> Rating
        "text" in parts -> Note
        parts.any { it == "drinkslug" || it == "customdrinkname" } -> Drink
        "visibility" in parts -> Visibility
        else -> null
    }
}

private fun HomePreparationField.defaultError() = when (this) {
    Date -> HomePreparationIssue.InvalidDate
    Method -> HomePreparationIssue.InvalidMethod
    Bean -> HomePreparationIssue.InvalidBean
    Dose -> HomePreparationIssue.InvalidDose
    Duration -> HomePreparationIssue.InvalidDuration
    Rating -> HomePreparationIssue.InvalidRating
    Note -> HomePreparationIssue.NoteTooLong
    Drink -> HomePreparationIssue.InvalidDrinkName
    Photos -> HomePreparationIssue.InvalidPhotos
    Visibility -> HomePreparationIssue.InvalidVisibility
}

// Exact old-server messages are transport compatibility keys, never user-facing text.
private val knownFieldErrors: Map<String, Pair<HomePreparationField, HomePreparationIssue>> = buildMap {
    fun add(field: HomePreparationField, message: HomePreparationIssue = field.defaultError(), vararg serverMessages: String) {
        serverMessages.forEach { put(it, field to message) }
    }
    add(Date, serverMessages = arrayOf(
        "PreparedAt must be an ISO 8601 timestamp with an explicit UTC offset or Z.",
        "PreparedOn must be within one calendar day of PreparedAt in UTC.",
        "PreparedOn must be the local preparation date.",
    ))
    add(Date, HomePreparationIssue.FutureDate,
        "PreparedAt must be a valid UTC time and cannot be in the future.")
    add(Method, serverMessages = arrayOf("Invalid home brew method.", "Unknown home brew method.", "Brew method not found in the catalog."))
    add(Bean, serverMessages = arrayOf(
        "CoffeeProductSlug and CustomBeanName cannot both be supplied.", "CoffeeProductSlug is too long.",
        "CoffeeProductId cannot be combined with CoffeeProductSlug or CustomBeanName.",
        "CoffeeProductId must match this entry's selected product.", "CoffeeProductId must not be empty.",
    ))
    add(Bean, HomePreparationIssue.BeanUnavailable, "Published coffee product not found.")
    add(Bean, HomePreparationIssue.InvalidBeanName, "Bean name must contain 1–250 characters for a selected product.")
    add(Dose, serverMessages = arrayOf("DoseGrams must be between 0.01 and 10000 with at most two decimal places."))
    add(Duration, serverMessages = arrayOf("BrewDurationSeconds must be between 1 and 604800."))
    add(Rating, serverMessages = arrayOf("TasteScore must be between 1 and 5."))
    add(Note, serverMessages = arrayOf("Text must not exceed 1000 characters."))
    add(Drink, serverMessages = arrayOf("Choose Other to specify a custom drink name.", "Drink slug is too long.",
        "Other requires a drink name of 1 to 100 characters.", "A custom drink name is only allowed for Other."))
    add(Drink, HomePreparationIssue.DrinkUnavailable, "Drink not found or inactive.")
    add(Visibility, serverMessages = arrayOf("Visibility must be Private or Public."))
    add(Photos, serverMessages = arrayOf(
        "Photos must be distinct, valid storage keys; at most five photos are allowed.",
        "DrinkPhoto requires an original photo and a separate valid storage key.",
        "One or more photos are already attached to content.",
        "One or more photos were not found, do not belong to you, or were already used.",
        "Check-in photos must belong to the author and cannot be shared or exceed five.",
        "Check-in photos must be distinct.", "A drink cutout requires its own PNG and an original photo.",
    ))
    add(Photos, HomePreparationIssue.PhotosNotClaimed,
        "Photos could not be claimed, please retry with the same uploads.")
    add(Photos, HomePreparationIssue.PhotosAlreadyUsed,
        "The uploads already belong to a committed check-in.")
}
