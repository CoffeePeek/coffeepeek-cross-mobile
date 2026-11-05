package com.coffeepeek.admin.di

import com.coffeepeek.admin.session.SessionStoreCredentials
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.auth.data.createTokenRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.dsl.module

/**
 * The account's four HTTP clients over the stored session. The session is read for every request; a rotated refresh token
 * is stored through [SessionStore], and the stored session is loaded as soon as the transport exists.
 */
internal fun transportModule(baseUrl: String, cacheFolderPath: String, debug: Boolean) = module {
    single {
        val sessions = get<SessionStore>()
        get<CoroutineScope>().launch { sessions.warmCache() }
        ApiClients(
            url = baseUrl,
            cacheFolderPath = cacheFolderPath,
            debug = debug,
            credentials = SessionStoreCredentials(sessions),
            refreshWith = { plain -> createTokenRefresh(plain)::refresh },
        )
    }
}
