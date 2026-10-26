package com.coffeepeek.feature.profile.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.profile.data.repository.createProfileAccountData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class AccountSessions(initial: Session? = Session("token", userId = "a")) : SessionReader {
    val state = MutableStateFlow(initial)
    override fun peekSession() = state.value
    override fun isActiveSession(session: Session?) = session != null && session.accessToken.isNotBlank()
    override fun observeSession() = state
    override suspend fun getSession() = state.value
}

internal class ProfileAccountFixture(scope: CoroutineScope) {
    val sessions = AccountSessions()
    val requests = mutableListOf<HttpRequestData>()
    val uploadRequests = mutableListOf<HttpRequestData>()
    var handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { accountResponse(profileJson()) }
    var uploadHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("", HttpStatusCode.OK) }
    private val api = HttpClientFactory(MockEngine { request ->
        requests += request
        handler(request)
    }).api("https://api.example")
    private val uploads = HttpClient(MockEngine { request ->
        uploadRequests += request
        uploadHandler(request)
    })
    val data = createProfileAccountData(api, uploads, sessions, scope)
    fun close() {
        api.close()
        uploads.close()
    }
}

internal fun MockRequestHandleScope.accountResponse(data: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond("""{"isSuccess":true,"data":$data}""", status, headersOf(HttpHeaders.ContentType, "application/json"))

internal fun profileJson(name: String = "Anna", about: String = "About") = """{
    "userName":"$name","email":"anna@example.invalid","about":"$about","avatarUrl":"avatar",
    "checkInCount":3,"addedShopsCount":2,"addedRoastersCount":1,"followersCount":4,
    "address":{"slug":"anna","canonicalPath":"/users/anna","revision":2,"isAlias":true}
}"""
