package com.coffeepeek.feature.authorblocking.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingEntry
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.impl.ui.compose.AuthorBlockingHost
import com.coffeepeek.feature.authorblocking.impl.ui.compose.BlockedAuthorsScreen

/** Composition supplies the existing shared presenter, not a second repository or mutation scope. */
fun createAuthorBlockingEntry(presenter: AuthorBlockingPresenter): AuthorBlockingEntry = DefaultAuthorBlockingEntry(presenter)

private class DefaultAuthorBlockingEntry(private val presenter: AuthorBlockingPresenter) : AuthorBlockingEntry {
    @Composable
    override fun Screen(onBack: () -> Unit, onLogin: () -> Unit, onRegister: () -> Unit) =
        BlockedAuthorsScreen(presenter, onBack, onLogin, onRegister)

    @Composable
    override fun Host(onLogin: () -> Unit, bottomClearanceDp: Float) = AuthorBlockingHost(presenter, onLogin, bottomClearanceDp)
}
