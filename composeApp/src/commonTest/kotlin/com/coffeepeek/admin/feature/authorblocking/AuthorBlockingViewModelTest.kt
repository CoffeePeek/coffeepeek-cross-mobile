package com.coffeepeek.admin.feature.authorblocking

import com.coffeepeek.admin.feature.authorblocking.api.AuthorReference
import com.coffeepeek.admin.feature.authorblocking.domain.*
import com.coffeepeek.admin.feature.authorblocking.ui.AuthorBlockingViewModel
import com.coffeepeek.admin.feature.community.ui.FeedSessions
import com.coffeepeek.domain.model.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorBlockingViewModelTest {
    private val author = AuthorReference("stable", "name", "Author")
    @Test fun cancellationNeverWritesAndSuccessDismissesWithoutAnotherAlert() = runTest {
        var calls = 0
        val repository = FakeBlockRepository { calls++; Result.success(Unit) }
        val vm = AuthorBlockingViewModel(repository, FeedSessions(Session("token", userId = "viewer")), backgroundScope)
        runCurrent()
        vm.requestBlock(author); assertNotNull(vm.prompt.value); vm.dismiss()
        assertEquals(0, calls)
        vm.requestBlock(author); vm.confirm(); runCurrent()
        assertEquals(1, calls); assertNull(vm.prompt.value)
    }
    @Test fun failureOffersRetryAndAccountSwitchCancelsPendingConfirmation() = runTest {
        var calls = 0
        val sessions = FeedSessions(Session("token", userId = "viewer"))
        val repository = FakeBlockRepository { calls++; Result.failure(IllegalStateException("Offline")) }
        val vm = AuthorBlockingViewModel(repository, sessions, backgroundScope)
        runCurrent(); vm.requestBlock(author); vm.confirm(); runCurrent()
        assertNotNull(vm.prompt.value?.error); assertFalse(vm.prompt.value!!.working)
        vm.confirm(); runCurrent(); assertEquals(2, calls)
        sessions.applySession(Session("other", userId = "other")); runCurrent()
        assertNull(vm.prompt.value); vm.confirm(); runCurrent(); assertEquals(2, calls)
    }
    @Test fun guestLoginDoesNotAutomaticallyConfirmBlock() = runTest {
        var calls = 0
        val sessions = FeedSessions(null)
        val vm = AuthorBlockingViewModel(FakeBlockRepository { calls++; Result.success(Unit) }, sessions, backgroundScope)
        runCurrent(); vm.requestBlock(author)
        assertTrue(vm.needsLogin.value); assertNull(vm.prompt.value)
        vm.loginHandled(); sessions.applySession(Session("token", userId = "viewer")); runCurrent()
        assertFalse(vm.needsLogin.value); assertNull(vm.prompt.value); assertEquals(0, calls)
    }
}

private class FakeBlockRepository(private val mutate: suspend () -> Result<Unit>) : AuthorBlockRepository {
    override val state = MutableStateFlow(AuthorBlocksState(signedIn = true, ready = true))
    override suspend fun refresh() = Result.success(Unit)
    override suspend fun setBlocked(id: String?, slug: String?, name: String, avatarUrl: String?, blocked: Boolean) = mutate()
}
