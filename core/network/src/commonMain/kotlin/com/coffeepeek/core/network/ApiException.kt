package com.coffeepeek.core.network

/** A server answer that is not a usable payload: the envelope says failure, or the body is missing. */
class ApiException(message: String?, override val statusCode: Int? = null) : Exception(), HttpStatusFailure {
    override val message: String = message?.takeIf { it.isNotBlank() } ?: "Сервер вернул некорректный ответ"
}
