package com.coffeepeek.admin.di.collections

import com.coffeepeek.core.session.SessionReader
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.CancellationException

private val collectionOwner = AttributeKey<String>("CollectionRequestOwner")

/** Guard immediately before the engine, after bearer authentication and every authentication retry. */
internal class CollectionsHttpClient(base: HttpClient, sessions: SessionReader, verifyBearer: Boolean = true) {
    val client = base.apply {
        val phase = PipelinePhase("CollectionAccountBeforeSend")
        sendPipeline.insertPhaseBefore(HttpSendPipeline.Engine, phase)
        sendPipeline.intercept(phase) {
            val expected = context.attributes.getOrNull(collectionOwner)
            if (expected != null) {
                val session = sessions.peekSession()?.takeIf(sessions::isActiveSession)
                if (session?.userId != expected || (verifyBearer && context.headers[HttpHeaders.Authorization] != "Bearer ${session.accessToken}"))
                    throw CancellationException("Collection account changed before send")
            }
        }
    }

    fun pin(request: HttpRequestBuilder, owner: String) { request.attributes.put(collectionOwner, owner) }
}
