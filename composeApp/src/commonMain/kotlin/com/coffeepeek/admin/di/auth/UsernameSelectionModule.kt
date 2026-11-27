package com.coffeepeek.admin.di.auth

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.api.UsernameSelectionGate
import com.coffeepeek.feature.auth.api.UsernameSelectionEntry
import com.coffeepeek.feature.auth.data.createUsernameSelectionRepository
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.UsernameSelectionAccount
import com.coffeepeek.feature.auth.domain.UsernameSelectionOwner
import com.coffeepeek.feature.auth.domain.UsernameSelectionRepository
import com.coffeepeek.feature.auth.impl.createUsernameSelectionGate
import com.coffeepeek.feature.auth.impl.createUsernameSelectionEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

internal fun usernameSelectionModule() = module {
    single<com.coffeepeek.feature.auth.api.LocalUserData> { UserSessionLocalUserData(get()) }
    single<com.coffeepeek.feature.auth.api.AuthAnalytics> { AppAuthAnalytics() }
    single<AuthRepository> {
        com.coffeepeek.feature.auth.data.createAuthRepository(get<ApiClients>().plainClient, get(), get())
    }
    single<UsernameSelectionAccount> { SessionUsernameSelectionAccount(get()) }
    single<UsernameSelectionRepository> {
        val clients = get<ApiClients>()
        createUsernameSelectionRepository(clients.client, get(), clients::refreshCredentials) {
            get<com.coffeepeek.feature.profile.api.ProfileAccountPort>().invalidateOwnProfile()
            get<com.coffeepeek.feature.profile.api.ProfileSnapshotStore>().clear()
        }
    }
    single<UsernameSelectionGate> { createUsernameSelectionGate(get(), get(), get<AuthRepository>()::signOut) }
    single<UsernameSelectionEntry> { createUsernameSelectionEntry(get()) }
}

private class SessionUsernameSelectionAccount(private val sessions: SessionReader) : UsernameSelectionAccount {
    override fun current() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
        ?.let { UsernameSelectionOwner(it, sessions.accountGeneration()) }
    override fun observe() = sessions.observeSession().map { current() }.distinctUntilChanged()
}
