package com.coffeepeek.domain.repository

import com.coffeepeek.core.session.SessionStore

/** Bridge while consumers move to SessionReader/SessionStore; delete once none use it. */
interface SessionRepository : SessionStore
