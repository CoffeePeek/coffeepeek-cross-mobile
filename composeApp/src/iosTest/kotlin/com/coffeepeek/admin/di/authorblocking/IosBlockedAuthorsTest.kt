package com.coffeepeek.admin.di.authorblocking

import com.coffeepeek.feature.authorblocking.api.AuthorBlockError
import com.coffeepeek.feature.authorblocking.api.AuthorBlockPrompt
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingListState
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.BlockedAuthorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosBlockedAuthorsTest {
    @Test
    fun observationUsesMainAndCancellationDoesNotCloseTheSharedPresenter() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val presenter = FakeBlockedAuthorsPresenter()
        val received = mutableListOf<IosBlockedAuthorsState>()
        val observation = IosBlockedAuthors(presenter).observe(received::add)
        try {
            assertTrue(received.isEmpty())
            runCurrent()
            assertEquals("author", received.single().items.single().id)
            observation.cancel()
            presenter.blocks.value = AuthorBlockingListState()
            runCurrent()
            assertEquals(1, received.size)
            assertFalse(presenter.closed)
        } finally {
            observation.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun observationForwardsSessionResetAndRemovesPreviousAuthorsAndPrompt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val presenter = FakeBlockedAuthorsPresenter()
        presenter.prompt.value = AuthorBlockPrompt(author, false)
        val received = mutableListOf<IosBlockedAuthorsState>()
        val observation = IosBlockedAuthors(presenter).observe(received::add)
        try {
            runCurrent()
            presenter.blocks.value = AuthorBlockingListState()
            presenter.prompt.value = null
            runCurrent()
            assertTrue(received.last().items.isEmpty())
            assertFalse(received.last().signedIn)
            assertNull(received.last().prompt)
        } finally {
            observation.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unblockResolvesCurrentIdentityAndRejectsStaleRowsOrAnUnavailableSession() {
        val presenter = FakeBlockedAuthorsPresenter()
        val adapter = IosBlockedAuthors(presenter)
        adapter.requestUnblock("author")
        assertEquals(author, presenter.requested)
        presenter.requested = null
        presenter.blocks.value = presenter.blocks.value.copy(items = emptyList())
        adapter.requestUnblock("author")
        assertNull(presenter.requested)
        presenter.blocks.value = signedInState().copy(ready = false)
        adapter.requestUnblock("author")
        assertNull(presenter.requested)
        presenter.blocks.value = signedInState().copy(signedIn = false)
        adapter.requestUnblock("author")
        assertNull(presenter.requested)
    }

    @Test
    fun confirmationAndRetryUseTheSamePresenterAndExposeWorkingAndFailure() {
        val presenter = FakeBlockedAuthorsPresenter()
        val adapter = IosBlockedAuthors(presenter)
        presenter.prompt.value = AuthorBlockPrompt(author, false, working = true)
        adapter.requestUnblock("author")
        assertNull(presenter.requested)
        assertTrue(adapter.currentState().prompt!!.working)
        presenter.prompt.value = AuthorBlockPrompt(author, false, error = AuthorBlockError.SaveFailed)
        assertTrue(adapter.currentState().prompt!!.failed)
        adapter.confirm(adapter.currentState().prompt!!)
        assertEquals(1, presenter.confirmations)
        adapter.dismiss(adapter.currentState().prompt!!)
        assertNull(adapter.currentState().prompt)
        presenter.needsLogin.value = true
        assertTrue(adapter.currentState().needsLogin)
        adapter.loginHandled()
        assertFalse(adapter.currentState().needsLogin)
    }

    @Test
    fun refreshDelegatesToTheExistingPresenter() {
        val presenter = FakeBlockedAuthorsPresenter()
        IosBlockedAuthors(presenter).refresh()
        assertEquals(1, presenter.refreshes)
    }

    @Test
    fun staleAlertCannotConfirmOrDismissAReplacementEvenWithTheSameAuthor() {
        val presenter = FakeBlockedAuthorsPresenter()
        val adapter = IosBlockedAuthors(presenter)
        presenter.prompt.value = AuthorBlockPrompt(author, false)
        val previous = adapter.currentState().prompt!!
        presenter.prompt.value = null
        presenter.prompt.value = AuthorBlockPrompt(author, false)
        adapter.confirm(previous)
        adapter.dismiss(previous)
        assertEquals(0, presenter.confirmations)
        val current = adapter.currentState().prompt!!
        assertNotEquals(previous.id, current.id)
        adapter.confirm(current)
        assertEquals(1, presenter.confirmations)
        presenter.prompt.value = null
        adapter.confirm(current)
        assertEquals(1, presenter.confirmations)
    }
}

private val author = AuthorReference("author", "anna", "Анна", "https://example.invalid/avatar.jpg")

private fun signedInState() = AuthorBlockingListState(
    items = listOf(BlockedAuthorItem("author", "Анна", "anna", author.avatarUrl)),
    signedIn = true,
    ready = true,
)

private class FakeBlockedAuthorsPresenter : AuthorBlockingPresenter {
    override val blocks = MutableStateFlow(signedInState())
    override val prompt = MutableStateFlow<AuthorBlockPrompt?>(null)
    override val needsLogin = MutableStateFlow(false)
    var requested: AuthorReference? = null
    var confirmations = 0
    var refreshes = 0
    var closed = false

    override fun requestBlock(author: AuthorReference) { requested = author }
    override fun requestUnblock(author: AuthorReference) { requested = author }
    override fun dismiss() { prompt.value = null }
    override fun loginHandled() { needsLogin.value = false }
    override fun refresh() { refreshes++ }
    override fun confirm() { confirmations++ }
    override fun close() { closed = true }
}
