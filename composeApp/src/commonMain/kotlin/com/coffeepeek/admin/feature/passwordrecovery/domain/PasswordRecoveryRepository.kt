package com.coffeepeek.admin.feature.passwordrecovery.domain

internal interface PasswordRecoveryRepository {
    suspend fun requestReset(email: String): Result<Unit>
}
