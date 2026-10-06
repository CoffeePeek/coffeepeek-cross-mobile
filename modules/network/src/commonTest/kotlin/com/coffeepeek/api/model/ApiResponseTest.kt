package com.coffeepeek.api.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApiResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesInfrastructureErrorWithoutCoffeePeekSuccessField() {
        // Given
        val payload =
            """{"status":"error","code":404,"message":"Application not found"}"""

        // When
        val response = json.decodeFromString<ApiResponse<Unit>>(payload)

        // Then
        assertFalse(response.isSuccess)
        assertEquals("Application not found", response.message)
    }

    @Test
    fun usesReadableMessageWhenErrorContractHasNoMessage() {
        // Given
        val payload = """{"status":"error","code":502}"""

        // When
        val response = json.decodeFromString<ApiResponse<Unit>>(payload)

        // Then
        assertFalse(response.isSuccess)
        assertEquals("Сервер вернул некорректный ответ", com.coffeepeek.api.utils.ApiException(response.message).message)
    }
}
