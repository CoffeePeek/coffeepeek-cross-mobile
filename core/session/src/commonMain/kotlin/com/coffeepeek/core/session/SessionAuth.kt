package com.coffeepeek.core.session

object SessionAuth {
    fun isActive(session: Session?): Boolean {
        if (session == null) return false
        if (session.accessToken.isBlank() || session.userId.isNullOrBlank()) return false
        if (!JwtUtils.isAccessTokenExpired(session.accessToken)) return true
        return !session.refreshToken.isNullOrBlank()
    }
}
