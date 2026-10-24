package com.coffeepeek.data.repository

import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.domain.repository.SessionRepository

/** Legacy name for the app's session store; goes away with [SessionRepository]. */
class SessionRepositoryImpl(private val store: SessionStore) : SessionRepository, SessionStore by store
