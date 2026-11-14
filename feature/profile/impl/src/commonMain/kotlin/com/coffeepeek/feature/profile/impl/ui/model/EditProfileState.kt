package com.coffeepeek.feature.profile.impl.ui.model

import com.coffeepeek.feature.profile.api.ProfileAvatarImage

internal enum class UsernameError { Empty, TooShort, TooLong }

internal enum class EditProfileError {
    Load, AvatarTooLarge, Avatar, Username, About, UsernameLimit, UsernameTaken, InvalidUsername, SignInRequired,
}

internal data class EditProfileState(
    val username: String = "",
    val about: String = "",
    val avatarUrl: String? = null,
    val pendingAvatar: ProfileAvatarImage? = null,
    val originalUsername: String = "",
    val originalAbout: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: EditProfileError? = null,
    val usernameRetryAtUtc: String? = null,
    val usernameToConfirm: String? = null,
) {
    /** Measured on the untrimmed value, as in production; the saved value is trimmed. */
    val usernameError: UsernameError? get() = when {
        username.isBlank() -> UsernameError.Empty
        username.length < MIN_USERNAME -> UsernameError.TooShort
        username.length > MAX_USERNAME -> UsernameError.TooLong
        else -> null
    }
    val aboutTooLong: Boolean get() = about.length > MAX_ABOUT
    val hasChanges: Boolean get() = username.trim() != originalUsername || about.trim() != originalAbout || pendingAvatar != null
    val canSave: Boolean get() = !isLoading && usernameError == null && !aboutTooLong && hasChanges && !isSaving

    companion object {
        const val MIN_USERNAME = 2
        const val MAX_USERNAME = 64
        const val MAX_ABOUT = 600
        const val MAX_AVATAR_BYTES = 5 * 1024 * 1024
    }
}

internal sealed interface EditProfileAction {
    data class UsernameChanged(val value: String) : EditProfileAction
    data class AboutChanged(val value: String) : EditProfileAction
    data class AvatarPicked(val image: ProfileAvatarImage) : EditProfileAction
    data object DismissError : EditProfileAction
    data object ConfirmUsernameChange : EditProfileAction
    data object CancelUsernameChange : EditProfileAction
    data object Save : EditProfileAction
}

internal sealed interface EditProfileEvent {
    /** Everything that changed was saved. */
    data object Saved : EditProfileEvent
}
