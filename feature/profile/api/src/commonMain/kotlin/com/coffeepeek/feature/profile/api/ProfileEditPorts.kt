package com.coffeepeek.feature.profile.api

/** A picked avatar, already read into memory by the app's picker. */
class ProfileAvatarImage(val fileName: String, val contentType: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = this === other ||
        (other is ProfileAvatarImage && fileName == other.fileName && contentType == other.contentType && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * (31 * fileName.hashCode() + contentType.hashCode()) + bytes.contentHashCode()
}

/**
 * Writes the signed-in user's own profile. Each call updates the shared cached profile on success, so the Profile tab follows without a
 * result being passed back. Failures carry a user-readable message when the server gave one.
 */
interface ProfileEditPort {
    suspend fun updateAvatar(image: ProfileAvatarImage): Result<Unit>
    suspend fun updateUsername(username: String): Result<Unit>
    suspend fun updateAbout(about: String): Result<Unit>
}

/** A pending account-deletion request. [resendAvailableAtUtc] is an ISO-8601 instant; blank or unparsable means "resend allowed". */
data class ProfileDeletionRequest(val resendAvailableAtUtc: String)

/** The server side of account deletion: the e-mail confirmation link is sent by [request]; the account stays active until it is used. */
interface AccountDeletionPort {
    /** `null` when no request exists. */
    suspend fun existingRequest(): Result<ProfileDeletionRequest?>

    /** Issues the request (sends the e-mail). Called only after an explicit user action. */
    suspend fun request(): Result<ProfileDeletionRequest>
}
