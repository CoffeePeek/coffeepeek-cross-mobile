package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.domain.UsernameSelectionOwner
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.CancellationException

/** Check the login lifetime again immediately before the engine sends a read or write. */
internal class UsernameSelectionTransport(base: HttpClient, sessions: SessionReader) {
    private val owner = AttributeKey<UsernameSelectionOwner>("UsernameSelectionOwner")
    val client = base.config {}.apply {
        val phase = PipelinePhase("UsernameSelectionAccount")
        sendPipeline.insertPhaseBefore(HttpSendPipeline.Engine, phase)
        sendPipeline.intercept(phase) {
            val expected = context.attributes.getOrNull(owner)
            val session = sessions.peekSession()
            if (expected != null && (session?.userId != expected.id || !sessions.isActiveSession(session) ||
                sessions.accountGeneration() != expected.generation ||
                context.headers[HttpHeaders.Authorization] != "Bearer ${session?.accessToken}"))
                throw CancellationException("Account changed before username request")
        }
    }
    fun pin(request: HttpRequestBuilder, expected: UsernameSelectionOwner) { request.attributes.put(owner, expected) }
}
