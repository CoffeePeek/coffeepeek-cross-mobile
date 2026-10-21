package com.coffeepeek.legacy.homecheckin.data

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInFailure
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInField
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInField.*
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal suspend fun <T> homeRequestResult(block: suspend () -> T): Result<T> = requestResult {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: HomeCheckInFailure) {
        throw error
    } catch (error: ResponseException) {
        throw homeServerFailure(error.response.status.value, error.response.bodyAsText())
    } catch (_: HttpRequestTimeoutException) {
        throw HomeCheckInFailure("Сервер не ответил вовремя. Попробуйте ещё раз.")
    } catch (_: IOException) {
        throw HomeCheckInFailure("Нет соединения с сервером. Проверьте интернет и попробуйте ещё раз.")
    } catch (_: SerializationException) {
        throw HomeCheckInFailure("Не удалось прочитать ответ сервера. Попробуйте ещё раз.")
    } catch (error: Exception) {
        // The application's shared HTTP validator can reject 5xx before we read the body.
        val message = if (error.message == "Сервис временно недоступен. Попробуйте чуть позже.")
            "Сервис временно недоступен. Попробуйте чуть позже."
        else "Не удалось выполнить запрос. Попробуйте ещё раз."
        throw HomeCheckInFailure(message)
    }
}

internal suspend fun <T> homePhotoUpload(block: suspend () -> T): T = homeRequestResult(block).getOrElse { failure ->
    val safe = failure as HomeCheckInFailure
    throw HomeCheckInFailure(safe.message, safe.fieldErrors, com.coffeepeek.legacy.homecheckin.domain.HomeCheckInOperation.PhotoUpload)
}

/** ErrorResponse and ASP.NET ValidationProblemDetails, as returned by the Shops service. */
internal fun homeServerFailure(status: Int, body: String): HomeCheckInFailure {
    val payload = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
    val code = payload.string("errorCode")
    // Infrastructure/authentication errors must not be mistaken for editable field errors.
    val general = when {
        status == 401 || code == "UNAUTHORIZED" -> "Сессия истекла. Войдите в аккаунт и сохраните чекин снова."
        status == 403 || code == "FORBIDDEN" -> "Нет доступа к этой записи. Проверьте, в какой аккаунт вы вошли."
        status == 429 -> "Слишком много запросов. Подождите немного и попробуйте ещё раз."
        status >= 500 || code in setOf("INTERNAL_ERROR", "SERVICE_UNAVAILABLE") ->
            "Сервис временно недоступен. Попробуйте чуть позже."
        else -> null
    }
    if (general != null) return HomeCheckInFailure(general)

    val fields = linkedMapOf<HomeCheckInField, String>()
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
    knownFieldErrors[payload.string("message")]?.let { (field, message) -> fields.getOrPut(field) { message } }
    return HomeCheckInFailure(
        when {
            fields.isNotEmpty() -> "Проверьте отмеченные поля и попробуйте ещё раз."
            status == 404 || code == "NOT_FOUND" -> "Запись не найдена"
            status == 409 || code == "CONFLICT" -> "Не удалось сохранить изменения: запись уже изменилась. Откройте её заново."
            status == 400 || status == 422 || code == "VALIDATION_FAILED" -> "Проверьте данные чекина и попробуйте ещё раз."
            else -> "Не удалось выполнить запрос. Попробуйте ещё раз."
        }, fields,
    )
}

private fun JsonObject?.value(key: String) = this?.entries?.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
private fun JsonObject?.string(key: String) = (value(key) as? JsonPrimitive)?.contentOrNull

private fun fieldForKey(key: String): HomeCheckInField? {
    val parts = key.lowercase().split('.', '[', ']')
    return when {
        "photos" in parts -> Photos
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

private fun HomeCheckInField.defaultError() = when (this) {
    Date -> "Выберите корректную дату приготовления"
    Method -> "Выберите доступный способ заваривания из списка"
    Bean -> "Выберите зерно заново или укажите своё название"
    Dose -> "Введите от 0,01 до 10 000 г, не более двух знаков после запятой"
    Duration -> "Укажите время от 1 секунды до 10 080 минут"
    Rating -> "Выберите оценку от 1 до 5"
    Note -> "Заметка должна быть не длиннее 1000 символов"
    Drink -> "Укажите название напитка: от 1 до 100 символов"
    Photos -> "Не удалось сохранить фотографии. Добавьте их заново (до 5 фото)."
    Visibility -> "Выберите видимость: только мне или всем"
}

// Older home endpoints return only a generic errorCode and a message. Keep these exact
// server messages in data; callers receive domain fields and safe Russian explanations.
private val knownFieldErrors: Map<String, Pair<HomeCheckInField, String>> = buildMap {
    fun add(field: HomeCheckInField, message: String = field.defaultError(), vararg serverMessages: String) {
        serverMessages.forEach { put(it, field to message) }
    }
    add(Date, serverMessages = arrayOf(
        "PreparedAt must be an ISO 8601 timestamp with an explicit UTC offset or Z.",
        "PreparedOn must be within one calendar day of PreparedAt in UTC.",
        "PreparedOn must be the local preparation date.",
    ))
    add(Date, "Дата и время приготовления не могут быть в будущем",
        "PreparedAt must be a valid UTC time and cannot be in the future.")
    add(Method, serverMessages = arrayOf("Invalid home brew method.", "Unknown home brew method.", "Brew method not found in the catalog."))
    add(Bean, serverMessages = arrayOf(
        "CoffeeProductSlug and CustomBeanName cannot both be supplied.", "CoffeeProductSlug is too long.",
        "CoffeeProductId cannot be combined with CoffeeProductSlug or CustomBeanName.",
        "CoffeeProductId must match this entry's selected product.", "CoffeeProductId must not be empty.",
    ))
    add(Bean, "Это зерно больше недоступно. Выберите другое или укажите своё название.", "Published coffee product not found.")
    add(Bean, "Название зерна должно содержать от 1 до 250 символов", "Bean name must contain 1–250 characters for a selected product.")
    add(Dose, serverMessages = arrayOf("DoseGrams must be between 0.01 and 10000 with at most two decimal places."))
    add(Duration, serverMessages = arrayOf("BrewDurationSeconds must be between 1 and 604800."))
    add(Rating, serverMessages = arrayOf("TasteScore must be between 1 and 5."))
    add(Note, serverMessages = arrayOf("Text must not exceed 1000 characters."))
    add(Drink, serverMessages = arrayOf("Choose Other to specify a custom drink name.", "Drink slug is too long.",
        "Other requires a drink name of 1 to 100 characters.", "A custom drink name is only allowed for Other."))
    add(Drink, "Этот напиток больше недоступен. Укажите название заново.", "Drink not found or inactive.")
    add(Visibility, serverMessages = arrayOf("Visibility must be Private or Public."))
    add(Photos, serverMessages = arrayOf(
        "Photos must be distinct, valid storage keys; at most five photos are allowed.",
        "One or more photos are already attached to content.",
        "One or more photos were not found, do not belong to you, or were already used.",
        "Check-in photos must belong to the author and cannot be shared or exceed five.",
        "Check-in photos must be distinct.",
    ))
    add(Photos, "Не удалось прикрепить фотографии. Попробуйте сохранить чекин ещё раз.",
        "Photos could not be claimed, please retry with the same uploads.")
    add(Photos, "Эти фотографии уже использованы в другом чекине. Добавьте другие.",
        "The uploads already belong to a committed check-in.")
}
