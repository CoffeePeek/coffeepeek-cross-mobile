package com.coffeepeek.feature.authorblocking

import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.api.BlockedAuthorItem
import com.coffeepeek.feature.authorblocking.domain.*
import com.coffeepeek.feature.authorblocking.ui.AuthorBlockingViewModel
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
        val vm = AuthorBlockingViewModel(repository, BlockSessions(Session("token", userId = "viewer")), backgroundScope)
        runCurrent()
        vm.requestBlock(author); assertNotNull(vm.prompt.value); vm.dismiss()
        assertEquals(0, calls)
        vm.requestBlock(author); vm.confirm(); runCurrent()
        assertEquals(1, calls); assertNull(vm.prompt.value)
    }
    @Test fun failureOffersRetryAndAccountSwitchCancelsPendingConfirmation() = runTest {
        var calls = 0
        val sessions = BlockSessions(Session("token", userId = "viewer"))
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
        val sessions = BlockSessions(null)
        val vm = AuthorBlockingViewModel(FakeBlockRepository { calls++; Result.success(Unit) }, sessions, backgroundScope)
        runCurrent(); vm.requestBlock(author)
        assertTrue(vm.needsLogin.value); assertNull(vm.prompt.value)
        vm.loginHandled(); sessions.applySession(Session("token", userId = "viewer")); runCurrent()
        assertFalse(vm.needsLogin.value); assertNull(vm.prompt.value); assertEquals(0, calls)
    }

    @Test fun publicPresenterPublishesListLoadingFailureAndClearedAccountState() = runTest {
        val repository = FakeBlockRepository { error("Rendering must not mutate blocks") }
        repository.state.value = AuthorBlocksState(items = listOf(BlockedAuthor("a", "Author", "author", "/avatar.png")),
            signedIn = true, ready = true)
        val presenter: AuthorBlockingPresenter = AuthorBlockingViewModel(repository, BlockSessions(), backgroundScope)
        assertEquals(listOf(BlockedAuthorItem("a", "Author", "author", "/avatar.png")), presenter.blocks.value.items)
        repository.state.value = repository.state.value.copy(loading = true, error = "Offline")
        runCurrent()
        assertTrue(presenter.blocks.value.loading)
        assertEquals("Offline", presenter.blocks.value.error)
        repository.state.value = AuthorBlocksState(signedIn = false, ready = true, sessionVersion = 2)
        runCurrent()
        assertTrue(presenter.blocks.value.ready)
        assertFalse(presenter.blocks.value.signedIn)
        assertTrue(presenter.blocks.value.items.isEmpty())
    }
}

private class FakeBlockRepository(private val mutate: suspend () -> Result<Unit>) : AuthorBlockRepository {
    override val state = MutableStateFlow(AuthorBlocksState(signedIn = true, ready = true))
    override suspend fun refresh() = Result.success(Unit)
    override suspend fun setBlocked(id: String?, slug: String?, name: String, avatarUrl: String?, blocked: Boolean) = mutate()
}
