package com.coffeepeek.data.session

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SessionTokenProvider(
    private val sessionRepository: SessionRepository,
    scope: CoroutineScope,
) {
    init {
        scope.launch { sessionRepository.warmCache() }
    }

    fun current(): AuthResp? {
        val session = sessionRepository.peekSession()
        return session?.takeIf { sessionRepository.isActiveSession(it) }?.toAuthResp()
    }

    private fun Session.toAuthResp() = AuthResp(
        accessToken = accessToken,
        refreshToken = refreshToken.orEmpty(),
    )
}
