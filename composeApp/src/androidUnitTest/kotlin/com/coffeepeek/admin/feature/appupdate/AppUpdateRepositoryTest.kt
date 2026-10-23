package com.coffeepeek.admin.feature.appupdate

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.feature.appupdate.data.AppUpdateRepositoryImpl
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** (Real time: the repository applies a 10 s timeout, which virtual time would trip while the mock engine answers on its own threads.)
 * The HTTP contract and the dismissed-version storage of the update check, as the legacy code implements them. */
class AppUpdateRepositoryTest {
    private class Settings : SettingRepository {
        val rows = mutableMapOf<String, String>()
        override suspend fun save(model: Setting) { rows[model.key] = model.value }
        override suspend fun read(key: String): Setting? = rows[key]?.let { Setting(key, it) }
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll() = rows.map { Setting(it.key, it.value) }
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) { rows.remove(key) }
    }

    private val body = """{"isSuccess":true,"data":{"versionCode":140,"minVersionCode":120,"url":"https://example.com/app.apk"}}"""
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(settings: Settings = Settings(), respond: (HttpRequestData) -> Pair<HttpStatusCode, String>, seen: MutableList<HttpRequestData> = mutableListOf()): AppUpdateRepositoryImpl {
        // The shared factory client is what production uses for the unauthenticated update check.
        val client = HttpClientFactory(MockEngine { request ->
            seen += request
            val (status, content) = respond(request)
            respond(content, status, json)
        }).api("https://api.example")
        return AppUpdateRepositoryImpl(client, settings)
    }

    @Test fun theRequestAsksTheBackendForThisPlatformAndChannelWithoutCaching() = runBlocking<Unit> {
        val seen = mutableListOf<HttpRequestData>()
        val update = repository(respond = { HttpStatusCode.OK to body }, seen = seen).fetch()
        assertEquals(AppUpdate(140, 120, "https://example.com/app.apk"), update)
        val request = seen.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/app-version", request.url.encodedPath)
        assertEquals(AppConfig.updatePlatform, request.url.parameters["platform"])
        assertEquals(AppConfig.updateChannel, request.url.parameters["channel"])
        assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
    }

    @Test fun anythingButAValidSuccessfulPolicyIsNoUpdate() = runBlocking<Unit> {
        assertNull(repository(respond = { HttpStatusCode.NoContent to "" }).fetch())
        assertNull(repository(respond = { HttpStatusCode.OK to """{"isSuccess":false,"data":null}""" }).fetch())
        assertNull(repository(respond = { HttpStatusCode.OK to body.replace("https://example.com", "http://example.com") }).fetch())
    }

    @Test fun errorStatusesAndUnreadableBodiesAreErrorsTheStateTurnsIntoAFailedCheck() = runBlocking<Unit> {
        assertFailsWith<Exception> { repository(respond = { HttpStatusCode.NotFound to body }).fetch() }
        assertFailsWith<Exception> { repository(respond = { HttpStatusCode.ServiceUnavailable to "" }).fetch() }
        assertFailsWith<Exception> { repository(respond = { HttpStatusCode.OK to "not json" }).fetch() }
    }

    @Test fun theDismissedVersionLivesInTheSettingsTable() = runBlocking<Unit> {
        val settings = Settings()
        val repository = repository(settings, { HttpStatusCode.OK to body })
        assertNull(repository.dismissedVersion())
        repository.dismiss(140)
        assertEquals("140", settings.rows["dismissed_app_version"])
        assertEquals(140L, repository.dismissedVersion())
        settings.rows["dismissed_app_version"] = "abc"
        assertNull(repository.dismissedVersion())
        settings.rows["dismissed_app_version"] = ""
        assertNull(repository.dismissedVersion())
        assertTrue(settings.rows.keys == setOf("dismissed_app_version"))
    }
}
