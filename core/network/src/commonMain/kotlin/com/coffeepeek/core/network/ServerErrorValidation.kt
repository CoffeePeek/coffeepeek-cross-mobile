package com.coffeepeek.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator

/** A server error (5xx) shown to the user as a temporary outage; the status stays available for diagnostics. */
class ServiceUnavailableException(override val statusCode: Int) :
    Exception("Сервис временно недоступен. Попробуйте чуть позже."), HttpStatusFailure

/** Server errors fail with [ServiceUnavailableException]; client errors (4xx) stay ordinary responses for business handling. */
internal fun HttpClientConfig<*>.installServerErrorValidation() {
    HttpResponseValidator {
        validateResponse { response ->
            if (response.status.value in 500..599) throw ServiceUnavailableException(response.status.value)
        }
    }
}
