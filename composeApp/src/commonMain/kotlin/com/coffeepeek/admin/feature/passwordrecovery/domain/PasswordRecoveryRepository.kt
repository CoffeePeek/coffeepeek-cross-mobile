package com.coffeepeek.admin.feature.passwordrecovery.domain

internal interface PasswordRecoveryRepository {
    suspend fun requestReset(email: String): Result<Unit>

    /** Accepts the reset token or the CoffeePeek reset link received by email. */
    suspend fun resetPassword(tokenOrLink: String, newPassword: String): Result<Unit>
}
