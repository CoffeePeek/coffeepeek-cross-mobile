package com.coffeepeek.admin.di.favorites

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.repository.FavoritesAccount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SessionFavoritesAccount(private val sessions: SessionReader) : FavoritesAccount {
    override val identities = sessions.observeSession().map { currentId() }.distinctUntilChanged()
    override fun currentId(): String? = sessions.favoritesAccountId()
}
