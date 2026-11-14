package com.coffeepeek.feature.settings.data

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*

internal class PreferenceSettings : SettingRepository {
    val values = MutableStateFlow<Map<String, Setting>>(emptyMap())
    override suspend fun save(model: Setting) { values.value += model.key to model }
    override suspend fun read(key: String) = values.value[key]
    override fun readFlow(key: String) = values.map { it[key] }
    override suspend fun readAll() = values.value.values.toList()
    override fun readAllFlow() = values.map { it.values.toList() }
    override suspend fun delete(key: String) { values.value -= key }
}

internal class PreferenceSessions : SessionReader {
    val state = MutableStateFlow<Session?>(Session("test", userId = "A"))
    var generation = 0L
    override fun accountGeneration() = generation
    override fun peekSession() = state.value
    override fun isActiveSession(session: Session?) = session != null && session == state.value
    override fun observeSession() = state
    override suspend fun getSession() = state.value
    fun switch(id: String?) { generation++; state.value = id?.let { Session("test-$it", userId = it) } }
}

internal class PreferenceFixture(val settings: PreferenceSettings = PreferenceSettings()) {
    val sessions = PreferenceSessions()
    val server = mutableMapOf("A" to true, "B" to true)
    val explicit = mutableSetOf<String>()
    val requests = mutableListOf<JsonObject>()
    var fail = false
    var responseOwner: String? = null
    var beforeResponse: suspend () -> Unit = {}
    val client = HttpClient(MockEngine { request ->
        beforeResponse()
        if (fail) return@MockEngine respond("{}", HttpStatusCode.ServiceUnavailable)
        val owner = sessions.peekSession()?.userId ?: "guest"
        val body = (request.body as? TextContent)?.text?.let { Json.parseToJsonElement(it).jsonObject }
        if (body != null) apply(body, owner)
        val json = """{"isSuccess":true,"data":{"accountId":"${responseOwner ?: owner}","enabled":${server[owner] ?: true}}}"""
        respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json() } }
    val repository = createAnalyticsPreferences(client, sessions, settings)

    private fun apply(body: JsonObject, owner: String) {
        requests += body
        check(body.getValue("accountId").jsonPrimitive.content == owner)
        if (!body.getValue("onlyIfUnset").jsonPrimitive.boolean || owner !in explicit) {
            server[owner] = body.getValue("enabled").jsonPrimitive.boolean
            explicit += owner
        }
    }
}
