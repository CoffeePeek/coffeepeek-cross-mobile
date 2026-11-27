package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileError
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState
import com.coffeepeek.feature.profile.impl.ui.model.UsernameError

/** Native rendering values; strings are existing profile resource keys. */
data class NativeEditProfileState(
    val username: String, val about: String, val avatarUrl: String?, val avatar: ProfileAvatarImage?,
    val loading: Boolean, val saving: Boolean, val canSave: Boolean, val revoked: Boolean,
    val usernameErrorKey: String?, val aboutErrorKey: String?, val usernameToConfirm: String?,
    val errorTitleKey: String?, val errorMessageKey: String?, val retryAtUtc: String?, val loadFailed: Boolean,
)

internal fun EditProfileState.nativeSnapshot(revoked: Boolean = false): NativeEditProfileState {
    if (revoked) return NativeEditProfileState("", "", null, null, false, false, false, true,
        null, null, null, null, null, null, false)
    return NativeEditProfileState(
        username, about, avatarUrl, pendingAvatar, isLoading, isSaving, canSave && !revoked, revoked,
        usernameError.takeIf { username.isNotEmpty() }?.resourceKey(),
        if (aboutTooLong) "profile_edit_about_long" else null, usernameToConfirm,
        error?.titleKey(), error?.messageKey(), usernameRetryAtUtc, error == EditProfileError.Load,
    )
}

private fun UsernameError.resourceKey() = when (this) {
    UsernameError.Empty -> "profile_edit_username_empty"
    UsernameError.TooShort -> "profile_edit_username_short"
    UsernameError.TooLong -> "profile_edit_username_long"
    UsernameError.Invalid -> "profile_edit_error_username_invalid"
}

private fun EditProfileError.titleKey() = when (this) {
    EditProfileError.UsernameLimit -> "profile_edit_error_limit_title"
    EditProfileError.UsernameTaken -> "profile_edit_error_taken_title"
    EditProfileError.Load -> "profile_edit_error_load_title"
    else -> "profile_edit_error_title"
}

private fun EditProfileError.messageKey() = when (this) {
    EditProfileError.Load -> "profile_edit_error_load"
    EditProfileError.AvatarTooLarge -> "profile_edit_error_avatar_size"
    EditProfileError.Avatar -> "profile_edit_error_avatar"
    EditProfileError.Username -> "profile_edit_error_username"
    EditProfileError.About -> "profile_edit_error_about"
    EditProfileError.UsernameLimit -> "profile_edit_error_username_limit"
    EditProfileError.UsernameTaken -> "profile_edit_error_username_taken"
    EditProfileError.InvalidUsername -> "profile_edit_error_username_invalid"
    EditProfileError.SignInRequired -> "profile_edit_error_sign_in"
}
