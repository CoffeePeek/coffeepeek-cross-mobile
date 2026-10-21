package com.coffeepeek.feature.authorblocking.impl

import com.coffeepeek.feature.authorblocking.api.AuthorBlockingAccount
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.data.createAuthorBlockData
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

/** Composition-only factory. Rendering and cross-feature consumers use the api contracts. */
fun createAuthorBlockingFeature(client: HttpClient, account: AuthorBlockingAccount): AuthorBlockingFeature {
    val data = createAuthorBlockData(client, account)
    return AuthorBlockingFeature(data.visibility, AuthorBlockingViewModel(data.repository, account), data::close)
}
