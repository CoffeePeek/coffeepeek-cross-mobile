package com.coffeepeek.feature.authorblocking

import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.api.BlockedAuthorItem
import com.coffeepeek.feature.authorblocking.domain.*
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingAccount
import com.coffeepeek.feature.authorblocking.impl.AuthorBlockingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorBlockingViewModelTest {
    private val author = AuthorReference("stable", "name", "Author")
    @Test fun cancellationNeverWritesAndSuccessDismissesWithoutAnotherAlert() = runTest {
        var calls = 0
        val repository = FakeBlockRepository { calls++; Result.success(Unit) }
        val vm = AuthorBlockingViewModel(repository, FakeAccount("viewer"), backgroundScope)
        runCurrent()
        vm.requestBlock(author); assertNotNull(vm.prompt.value); vm.dismiss()
        assertEquals(0, calls)
        vm.requestBlock(author); vm.confirm(); runCurrent()
        assertEquals(1, calls); assertNull(vm.prompt.value)
    }
    @Test fun failureOffersRetryAndAccountSwitchCancelsPendingConfirmation() = runTest {
        var calls = 0
        val sessions = FakeAccount("viewer")
        val repository = FakeBlockRepository { calls++; Result.failure(IllegalStateException("Offline")) }
        val vm = AuthorBlockingViewModel(repository, sessions, backgroundScope)
        runCurrent(); vm.requestBlock(author); vm.confirm(); runCurrent()
        assertNotNull(vm.prompt.value?.error); assertFalse(vm.prompt.value!!.working)
        vm.confirm(); runCurrent(); assertEquals(2, calls)
        sessions.switchTo("other"); runCurrent()
        assertNull(vm.prompt.value); vm.confirm(); runCurrent(); assertEquals(2, calls)
    }
    @Test fun guestLoginDoesNotAutomaticallyConfirmBlock() = runTest {
        var calls = 0
        val sessions = FakeAccount(null)
        val vm = AuthorBlockingViewModel(FakeBlockRepository { calls++; Result.success(Unit) }, sessions, backgroundScope)
        runCurrent(); vm.requestBlock(author)
        assertTrue(vm.needsLogin.value); assertNull(vm.prompt.value)
        vm.loginHandled(); sessions.switchTo("viewer"); runCurrent()
        assertFalse(vm.needsLogin.value); assertNull(vm.prompt.value); assertEquals(0, calls)
    }

    @Test fun publicPresenterPublishesListLoadingFailureAndClearedAccountState() = runTest {
        val repository = FakeBlockRepository { error("Rendering must not mutate blocks") }
        repository.state.value = AuthorBlocksState(items = listOf(BlockedAuthor("a", "Author", "author", "/avatar.png")),
            signedIn = true, ready = true)
        val presenter: AuthorBlockingPresenter = AuthorBlockingViewModel(repository, FakeAccount(), backgroundScope)
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

    @Test fun replacementBeforeAccountObserverRunsCannotConfirmTheOldPrompt() = runTest {
        var calls = 0
        val sessions = FakeAccount("viewer")
        val repository = FakeBlockRepository {
            calls++
            Result.success(Unit)
        }
        val vm = AuthorBlockingViewModel(repository, sessions, backgroundScope)
        runCurrent()
        vm.requestBlock(author)
        sessions.switchTo("other")
        vm.confirm()
        runCurrent()
        assertNull(vm.prompt.value)
        assertEquals(0, calls)
    }

    @Test fun firstObservedAccountReplacementClearsAnAlreadyRequestedPrompt() = runTest {
        val sessions = FakeAccount("viewer")
        val vm = AuthorBlockingViewModel(FakeBlockRepository { error("No confirmation") }, sessions, backgroundScope)
        vm.requestBlock(author)
        sessions.switchTo("other")
        runCurrent()
        assertNull(vm.prompt.value)
        assertFalse(vm.needsLogin.value)
    }

    @Test fun replacementAfterConfirmBeforeTheMutationStartsCannotWrite() = runTest {
        var calls = 0
        val sessions = FakeAccount("viewer")
        val repository = FakeBlockRepository {
            calls++
            Result.success(Unit)
        }
        val vm = AuthorBlockingViewModel(repository, sessions, backgroundScope)
        runCurrent()
        vm.requestBlock(author)
        vm.confirm()
        sessions.switchTo("other")
        runCurrent()
        assertEquals(0, calls)
        assertNull(vm.prompt.value)
    }

    @Test fun oldMutationCancellationCannotDismissANewAccountsPrompt() = runTest {
        val finishing = CompletableDeferred<Unit>()
        val sessions = FakeAccount("viewer")
        val vm = AuthorBlockingViewModel(FakeBlockRepository {
            awaitCancelledMutation(finishing)
        }, sessions, backgroundScope)
        runCurrent()
        vm.requestBlock(author)
        vm.confirm()
        runCurrent()
        sessions.switchTo("other")
        runCurrent()
        val otherAuthor = AuthorReference("other-author", "other-slug", "Другой автор")
        vm.requestBlock(otherAuthor)
        finishing.complete(Unit)
        runCurrent()
        assertEquals(otherAuthor, vm.prompt.value?.author)
        assertFalse(vm.prompt.value!!.working)
    }
}

private suspend fun awaitCancelledMutation(finishing: CompletableDeferred<Unit>): Result<Unit> {
    try {
        awaitCancellation()
    } finally {
        withContext(NonCancellable) { finishing.await() }
    }
}

private class FakeBlockRepository(private val mutate: suspend () -> Result<Unit>) : AuthorBlockRepository {
    override val state = MutableStateFlow(AuthorBlocksState(signedIn = true, ready = true))
    override suspend fun refresh() = Result.success(Unit)
    override suspend fun setBlocked(id: String?, slug: String?, name: String, avatarUrl: String?, blocked: Boolean) = mutate()
}

internal class FakeAccount(initial: String? = "a") : AuthorBlockingAccount {
    private val state = MutableStateFlow(initial)
    override fun current() = state.value
    override val changes: Flow<String?> = state
    fun switchTo(key: String?) { state.value = key }
}
