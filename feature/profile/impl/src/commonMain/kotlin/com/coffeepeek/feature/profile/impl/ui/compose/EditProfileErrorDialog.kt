package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_about
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_avatar
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_avatar_size
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_dismiss
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_limit_title
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_load
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_load_title
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_sign_in
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_taken_title
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_title
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_username
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_username_invalid
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_username_limit
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_username_retry_at
import com.coffeepeek.feature.profile.impl.resources.profile_edit_error_username_taken
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekErrorDialog
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileError
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState

@Composable
internal fun EditProfileErrorDialog(state: EditProfileState, onDismiss: () -> Unit) {
    val error = state.error ?: return
    val retryAt = localizedUsernameRetryAt(state.usernameRetryAtUtc)
    CoffeePeekErrorDialog(
        show = true,
        title = stringResource(error.titleResource()),
        message = if (error == EditProfileError.UsernameLimit && retryAt != null) {
            stringResource(Res.string.profile_edit_error_username_retry_at, retryAt)
        } else stringResource(error.messageResource()),
        dismissLabel = stringResource(Res.string.profile_edit_error_dismiss),
        onDismiss = onDismiss,
    )
}

private fun EditProfileError.titleResource(): StringResource = when (this) {
    EditProfileError.UsernameLimit -> Res.string.profile_edit_error_limit_title
    EditProfileError.UsernameTaken -> Res.string.profile_edit_error_taken_title
    EditProfileError.Load -> Res.string.profile_edit_error_load_title
    else -> Res.string.profile_edit_error_title
}

private fun EditProfileError.messageResource(): StringResource = when (this) {
    EditProfileError.Load -> Res.string.profile_edit_error_load
    EditProfileError.AvatarTooLarge -> Res.string.profile_edit_error_avatar_size
    EditProfileError.Avatar -> Res.string.profile_edit_error_avatar
    EditProfileError.Username -> Res.string.profile_edit_error_username
    EditProfileError.About -> Res.string.profile_edit_error_about
    EditProfileError.UsernameLimit -> Res.string.profile_edit_error_username_limit
    EditProfileError.UsernameTaken -> Res.string.profile_edit_error_username_taken
    EditProfileError.InvalidUsername -> Res.string.profile_edit_error_username_invalid
    EditProfileError.SignInRequired -> Res.string.profile_edit_error_sign_in
}

@Composable
private fun EditProfileErrorDialogPreview(dark: Boolean) {
    CoffeePeekTheme(darkTheme = dark) {
        EditProfileErrorDialog(
            EditProfileState(error = EditProfileError.UsernameLimit, usernameRetryAtUtc = "2026-10-29T17:12:31Z"), {},
        )
    }
}

/** The retry moment in the user's locale and time zone, medium date and short time; null when it cannot be read. */
internal expect fun localizedUsernameRetryAt(value: String?): String?

@Preview @Composable private fun EditProfileErrorDialogLightPreview() = EditProfileErrorDialogPreview(false)
@Preview @Composable private fun EditProfileErrorDialogDarkPreview() = EditProfileErrorDialogPreview(true)
