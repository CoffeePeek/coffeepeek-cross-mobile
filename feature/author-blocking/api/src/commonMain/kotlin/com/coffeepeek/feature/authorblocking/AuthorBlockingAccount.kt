package com.coffeepeek.feature.authorblocking.api

import kotlinx.coroutines.flow.Flow

/**
 * Identity of the signed-in account, supplied by the application. The key is opaque: it only has to
 * differ between accounts and be null when nobody is signed in or the session is no longer active.
 */
interface AuthorBlockingAccount {
    fun current(): String?
    val changes: Flow<String?>
}
