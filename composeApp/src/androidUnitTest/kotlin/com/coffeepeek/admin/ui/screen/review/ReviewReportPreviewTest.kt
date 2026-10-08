package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.CheckInHelpfulVote
import com.coffeepeek.domain.model.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.fail

class ReviewReportPreviewTest {
    @Test
    fun checkInReportUsesCheckInApiAndTrimsTheReason() = runBlocking {
        val requests = Channel<Pair<String, String>>(Channel.UNLIMITED)
        val checkIns = object : CheckInRepository {
            override suspend fun report(id: String, text: String): Result<Unit> {
                requests.send(id to text)
                return Result.success(Unit)
            }
            override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = error("unused")
            override suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>> = error("unused")
            override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> = error("unused")
            override suspend fun updateCheckIn(id: String, input: UpdateCheckInInput): Result<CheckIn> = error("unused")
            override suspend fun setVisibility(id: String, visibility: CheckInVisibility): Result<CheckIn> = error("unused")
            override suspend fun setHelpful(id: String, helpful: Boolean): Result<CheckInHelpfulVote> = error("unused")
        }
        val sessions = requireNotNull(SessionRepository::class.java.cast(Proxy.newProxyInstance(
            SessionRepository::class.java.classLoader, arrayOf(SessionRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getSession" -> Session("token")
                "isActiveSession" -> true
                else -> fail("Unexpected session call: ${method.name}")
            }
        }))
        val vm = ReviewReportViewModel("visit", unusedRepository(ReviewRepository::class.java), sessions,
            checkIns = checkIns, isCheckIn = true)
        try {
            vm.updateText("  Спам  ")
            vm.submit()
            assertEquals("visit" to "Спам", withTimeout(5_000) { requests.receive() })
            val sent = withTimeout(5_000) { vm.state.first { it.isSubmitted } }
            assertFalse(sent.isSubmitting)
            assertNull(sent.error)
            assertEquals("", sent.text)
        } finally { vm.close() }
    }

    @Test
    fun previewValidatesTheFormWithoutCallingAuthenticationOrTheApi() {
        val vm = ReviewReportViewModel(
            reviewId = "preview-maria",
            reviews = unusedRepository(ReviewRepository::class.java),
            sessions = unusedRepository(SessionRepository::class.java),
            isPreview = true,
            checkIns = unusedRepository(CheckInRepository::class.java),
        )

        vm.updateText("   ")
        vm.submit()
        assertFalse(vm.state.value.isSubmitted)
        assertNotNull(vm.state.value.error)

        vm.updateText("Пример причины жалобы")
        vm.submit()
        vm.submit()
        assertTrue(vm.state.value.isPreview)
        assertTrue(vm.state.value.isSubmitted)
        assertFalse(vm.state.value.isSubmitting)
        assertNull(vm.state.value.error)
    }

    private fun <T : Any> unusedRepository(type: Class<T>): T = requireNotNull(type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            fail("Preview must not call ${type.simpleName}.${method.name}")
        },
    ))
}
