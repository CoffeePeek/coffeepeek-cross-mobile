package com.coffeepeek.core.media.image

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoSessionInvalidationTest {
    @Test fun defaultVisibilityStillObservesCredentialRotationAccountChangeAndLogout() = runTest {
        val first = Session("access-a", "refresh-a", "viewer-a")
        val refreshed = first.copy(accessToken = "access-b")
        val other = Session("access-c", "refresh-c", "viewer-b")
        val repository = Sessions(first)
        val observed = mutableListOf<Session?>()
        val client = HttpClient(MockEngine { error("No network request is expected") })
        try {
            installPhotoSessionInvalidation(repository, client, backgroundScope, observed::add, {})
            runCurrent()
            for (session in listOf(refreshed, other, null)) {
                repository.applySession(session)
                runCurrent()
            }
            assertEquals(listOf(first, refreshed, other, null), observed)
        } finally { client.close() }
    }

    @Test fun defaultVisibilityRevokesBeforeAndAfterEveryPhotoMutationIncludingALostResponse() = runTest {
        val mutations = listOf(
            HttpMethod.Post to "/api/v1/check-ins",
            HttpMethod.Patch to "/api/v1/check-ins/visit/visibility",
            HttpMethod.Delete to "/api/v1/check-ins/home/home",
            HttpMethod.Put to "/api/v1/home-check-ins/home",
            HttpMethod.Put to "/api/v1/check-ins/helpful",
            HttpMethod.Delete to "/api/v1/check-ins/visit/helpful/photos/photo",
            HttpMethod.Post to "/api/v1/check-ins/visit/helpful",
            HttpMethod.Post to "/api/v1/users/me/blocks",
            HttpMethod.Put to "/api/Users/me",
        )
        var invalidations = 0
        var expectedBefore = 0
        val client = HttpClient(MockEngine {
            assertEquals(expectedBefore + 1, invalidations)
            if (it.method == HttpMethod.Delete) error("Synthetic lost mutation response")
            respond("", HttpStatusCode.OK)
        })
        try {
            installPhotoSessionInvalidation(Sessions(null), client, backgroundScope, {}, { invalidations++ })
            for ((method, path) in mutations) {
                expectedBefore = invalidations
                if (method == HttpMethod.Delete) {
                    assertFailsWith<IllegalStateException> {
                        client.request("https://gateway.example$path") { this.method = method }
                    }
                } else client.request("https://gateway.example$path") { this.method = method }
                assertEquals(expectedBefore + 2, invalidations)
            }
        } finally { client.close() }
    }

    @Test fun likesAndUnlikesDoNotRevokePhotosEvenWhenTheRequestFails() = runTest {
        var invalidations = 0
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            if (it.url.parameters["lost-response"] != null) error("Synthetic lost vote response")
            respond("", if (it.url.parameters["denied"] != null) HttpStatusCode.Forbidden else HttpStatusCode.OK)
        })
        try {
            installPhotoSessionInvalidation(Sessions(null), client, backgroundScope, {}, { invalidations++ })
            for (method in listOf(HttpMethod.Put, HttpMethod.Delete)) {
                for (path in listOf(
                    "/api/v1/check-ins/shop-visit/helpful",
                    "/API/v1/CHECK-INS/home-visit/HELPFUL/?denied=true",
                    "/api/v1/check-ins/visit/helpful?lost-response=true",
                )) {
                    if ("lost-response" in path) {
                        assertFailsWith<IllegalStateException> {
                            client.request("https://gateway.example$path") { this.method = method }
                        }
                    } else client.request("https://gateway.example$path") { this.method = method }
                    assertEquals(0, invalidations, "$method $path must keep the current photo permission")
                }
            }
            assertEquals(6, requests)
        } finally { client.close() }
    }

    @Test fun explicitAuthorVisibilityFlowKeepsImmediateLocalRevocation() = runTest {
        val visibility = MutableStateFlow(0)
        var invalidations = 0
        val client = HttpClient(MockEngine { error("No network request is expected") })
        try {
            installPhotoSessionInvalidation(Sessions(null), client, backgroundScope, {}, { invalidations++ }, visibility)
            runCurrent()
            assertEquals(0, invalidations)
            visibility.value = 1
            runCurrent()
            assertEquals(1, invalidations)
            visibility.value = 2
            runCurrent()
            assertEquals(2, invalidations)
        } finally { client.close() }
    }

    @Test fun readsAndUnrelatedWritesDoNotRotatePhotoPermission() = runTest {
        var invalidations = 0
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        try {
            installPhotoSessionInvalidation(Sessions(null), client, backgroundScope, {}, { invalidations++ })
            client.request("https://gateway.example/api/v1/check-ins/visit/photos/photo") { method = HttpMethod.Get }
            client.request("https://gateway.example/api/Catalogs/cities") { method = HttpMethod.Post }
            assertEquals(0, invalidations)
        } finally { client.close() }
    }

    private class Sessions(initial: Session?) : SessionReader {
        private val current = MutableStateFlow(initial)
        override fun peekSession() = current.value
        fun applySession(session: Session?) { current.value = session }
        override fun isActiveSession(session: Session?) = current.value == session
        override suspend fun getSession() = current.value
        override fun observeSession() = current
    }
}
