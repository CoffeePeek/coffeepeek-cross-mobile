@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

/** Composition factory; transport and session details do not escape auth/data. */
fun createUsernameSelectionRepository(client: HttpClient, sessions: SessionReader,
    refresh: suspend () -> Boolean,
    profileReady: (UsernameSelectionOwner) -> Unit = {},
): UsernameSelectionRepository {
    return UsernameSelectionRepositoryImpl(client, sessions, refresh, profileReady)
}

internal class UsernameSelectionRepositoryImpl(client: HttpClient,
    private val sessions: SessionReader, private val refresh: suspend () -> Boolean,
    private val profileReady: (UsernameSelectionOwner) -> Unit,
) : UsernameSelectionRepository {
    private val transport = UsernameSelectionTransport(client, sessions)

    override suspend fun load(owner: UsernameSelectionOwner) = requestResult {
        verify(owner)
        val profile = read(owner)
        if (!profile.required) {
            synchronize(owner, profile.username)
            profileReady(owner)
            verify(owner)
        }
        profile
    }

    override suspend fun complete(owner: UsernameSelectionOwner, username: String) = requestResult {
        verify(owner)
        if (!isValidSelectedUsername(username)) throw UsernameSelectionException(UsernameSelectionFailure.Invalid)
        val response = transport.client.put("/api/Users/me/username-selection") {
            transport.pin(this, owner)
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(SelectedUsernameDto(username.trim()))
        }
        verify(owner)
        response.checkedEnvelope<JsonElement>()
        load(owner).getOrThrow().also {
            if (it.required) throw UsernameSelectionException(UsernameSelectionFailure.Unavailable)
        }
    }

    private suspend fun read(owner: UsernameSelectionOwner): UsernameSelectionProfile {
        val response = transport.client.get("/api/Users/me") {
            transport.pin(this, owner)
            expectSuccess = false
            header(HttpHeaders.CacheControl, "no-cache, no-store")
        }
        verify(owner)
        val data = response.checkedEnvelope<SelectionProfileDto>()
        return UsernameSelectionProfile(data.userName, data.usernameSelectionRequired)
    }

    private suspend fun synchronize(owner: UsernameSelectionOwner, username: String) {
        verify(owner)
        if (tokenUsername() == username) return
        if (!refresh()) throw UsernameSelectionException(UsernameSelectionFailure.Unavailable)
        verify(owner)
        if (tokenUsername() != username) throw UsernameSelectionException(UsernameSelectionFailure.Unavailable)
    }

    private fun verify(owner: UsernameSelectionOwner) {
        val session = sessions.peekSession()
        if (!sessions.isActiveSession(session) || session?.userId != owner.id || sessions.accountGeneration() != owner.generation)
            throw CancellationException("Account changed during username selection")
    }

    private fun tokenUsername(): String? {
        val payload = sessions.peekSession()?.accessToken?.split('.')?.getOrNull(1) ?: return null
        return try {
            val json = Json.parseToJsonElement(Base64.UrlSafe.decode(payload.padEnd((payload.length + 3) / 4 * 4, '=')).decodeToString()).jsonObject
            json["name"]?.jsonPrimitive?.contentOrNull ?: json["preferred_username"]?.jsonPrimitive?.contentOrNull
        } catch (_: IllegalArgumentException) { null }
    }
}

private suspend inline fun <reified T> HttpResponse.checkedEnvelope(): T {
    val envelope = body<Envelope<T>>()
    if (status.isSuccess() && envelope.isSuccess && envelope.data != null) return envelope.data
    val message = envelope.message.orEmpty().lowercase()
    val failure = when {
        envelope.errorCode == "USERNAME_TAKEN" || "taken" in message || "already exists" in message || "занят" in message -> UsernameSelectionFailure.Taken
        status == HttpStatusCode.Conflict -> UsernameSelectionFailure.Conflict
        status == HttpStatusCode.BadRequest -> UsernameSelectionFailure.Invalid
        else -> UsernameSelectionFailure.Unavailable
    }
    throw UsernameSelectionException(failure)
}

@Serializable
private data class SelectionProfileDto(@JsonNames("username", "UserName") val userName: String,
    val usernameSelectionRequired: Boolean = false)
@Serializable
private data class SelectedUsernameDto(val username: String)
