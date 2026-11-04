package com.coffeepeek.admin.di.session

import com.coffeepeek.core.session.SessionReader
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.CancellationException

/** Shared application adapter: verify the pinned account after authentication and every retry. */
internal class SessionPinnedHttpClient(base: HttpClient, sessions: SessionReader, name: String, verifyBearer: Boolean = true) {
    private val owner = AttributeKey<String>("${name}RequestOwner")
    val client = base.apply {
        val phase = PipelinePhase("${name}AccountBeforeSend")
        sendPipeline.insertPhaseBefore(HttpSendPipeline.Engine, phase)
        sendPipeline.intercept(phase) {
            val expected = context.attributes.getOrNull(owner)
            if (expected != null) {
                val session = sessions.peekSession()?.takeIf(sessions::isActiveSession)
                if (session?.userId != expected || (verifyBearer && context.headers[HttpHeaders.Authorization] != "Bearer ${session.accessToken}"))
                    throw CancellationException("Account changed before send")
                if (!verifyBearer) {
                    context.headers.remove("X-CoffeePeek-Sandbox-Owner")
                    context.headers.append("X-CoffeePeek-Sandbox-Owner", expected)
                }
            }
        }
    }

    fun pin(request: HttpRequestBuilder, expected: String) { request.attributes.put(owner, expected) }
}
