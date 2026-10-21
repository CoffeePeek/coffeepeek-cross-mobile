package com.coffeepeek.feature.authorblocking.api

import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.authorblocking.data.AuthorBlockRepositoryImpl
import com.coffeepeek.feature.authorblocking.ui.AuthorBlockingViewModel
import io.ktor.client.HttpClient

/** Application DI owns the lifecycle; implementation classes remain inside the feature. */
class AuthorBlockingFeature internal constructor(
    val visibility: AuthorVisibility,
    val presenter: AuthorBlockingPresenter,
    private val closeRepository: () -> Unit,
) : AutoCloseable {
    override fun close() {
        presenter.close()
        closeRepository()
    }
}

/** Composition-only factory. Rendering and cross-feature consumers use the contracts above. */
fun createAuthorBlockingFeature(client: HttpClient, sessions: SessionRepository): AuthorBlockingFeature {
    val repository = AuthorBlockRepositoryImpl(client, sessions)
    return AuthorBlockingFeature(repository, AuthorBlockingViewModel(repository, sessions), repository::close)
}
