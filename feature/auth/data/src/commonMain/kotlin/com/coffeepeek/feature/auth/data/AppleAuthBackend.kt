package com.coffeepeek.feature.auth.data

import com.coffeepeek.feature.auth.domain.AppleSignInChallenge
import com.coffeepeek.core.network.HttpStatusFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import io.ktor.serialization.ContentConvertException

/** Apple keeps the existing nonce/state wire contract and ASI diagnostics; tokens never escape data. */
internal class AppleAuthBackend(private val client: HttpClient) {
    suspend fun challenge(): AppleSignInChallenge {
        val response = client.post("/api/Tokens/apple/challenge")
        val envelope = response.body<Envelope<AppleChallengeDto>>()
        val data = envelope.data
        if (!response.status.isSuccess() || !envelope.isSuccess || data == null || data.challengeId.isBlank() || data.nonce.isBlank())
            throw AuthApiException(envelope.message)
        return AppleSignInChallenge(data.challengeId, data.nonce)
    }

    suspend fun signIn(challengeId: String, idToken: String): TokensDto {
        val response = loginResponse(AppleLoginRequest(challengeId, idToken))
        val envelope = try { response.body<Envelope<AppleTokensDto>>() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: ContentConvertException) { null }
        catch (_: NoTransformationFoundException) { null }
        if (!response.status.isSuccess()) throw AuthApiException("${envelope?.message.orEmpty()} [ASI-H${response.status.value}]", response.status.value)
        if (envelope == null) throw AuthApiException("Invalid Apple response [ASI-R]")
        if (!envelope.isSuccess) throw AuthApiException("${envelope.message.orEmpty()} [ASI-A]")
        val access = envelope.data?.accessToken?.takeIf(String::isNotBlank)
        val refresh = envelope.data?.refreshToken?.takeIf(String::isNotBlank)
        if (access == null || refresh == null) throw AuthApiException("Missing Apple session [ASI-I]")
        return TokensDto(access, refresh)
    }

    private suspend fun loginResponse(request: AppleLoginRequest): HttpResponse = try {
        client.post("/api/Tokens/apple/login") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        val status = (failure as? HttpStatusFailure)?.statusCode ?: (failure as? ResponseException)?.response?.status?.value
        if (status != null && status in 400..599) throw AuthApiException("${failure.message.orEmpty()} [ASI-H$status]", status)
        throw failure
    }
}

@Serializable
private data class AppleChallengeDto(val challengeId: String, val nonce: String)

@Serializable
private data class AppleLoginRequest(val challengeId: String, val idToken: String)

@Serializable
private data class AppleTokensDto(val accessToken: String? = null, val refreshToken: String? = null)
